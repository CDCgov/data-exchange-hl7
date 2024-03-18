package gov.cdc.dex.hl7

import com.google.gson.*
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.util.ProcessingStatus.PSClientUtility
import gov.cdc.hl7.bumblebee.HL7JsonTransformer
import org.slf4j.LoggerFactory
import java.util.*


/**
 * Azure function with event hub trigger for the HL7 JSON Lake transformer
 * Takes an HL7 message and converts it to an HL7 json lake based on the PhinGuidProfile
 */
class Function {
    companion object {
        const val PROCESS_STATUS_OK = "SUCCESS"
        const val PROCESS_STATUS_EXCEPTION = "FAILURE"
        const val SUMMARY_STATUS_OK = "HL7-JSON-LAKE-TRANSFORMED"
        const val SUMMARY_STATUS_ERROR = "HL7-JSON-LAKE-ERROR"
        val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create()
        val gsonNoNulls: Gson = GsonBuilder().create()
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        val fnConfig = FunctionConfig()
    } // .companion object

    @FunctionName("HL7_JSON_LAKE_TRANSFORMER")
    fun eventHubProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            consumerGroup = "%EventHubConsumerGroup%",
            connection = "EventHubConnectionString"
        )
        messages: List<String>?,
        @BindingName("SystemPropertiesArray") eventHubMD: List<EventHubMetadata>,
        context: ExecutionContext
    ): JsonObject {

        logger.info("DEX::${context.functionName}")
        if (messages == null) {
            return JsonObject()
        }
        val outList = mutableListOf<String>()
        var returnValue = JsonObject()
        val psClientUtility = PSClientUtility()
        var traceId :String =""
        var spanId :String =""
        var childSpanId :String =""
        try {
            messages.forEachIndexed { messageIndex: Int, singleMessage: String? ->
                val startTime = Date().toIsoString()
                try {
                    val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                    val filePath = JsonHelper.getValueFromJson("routing_metadata.ingested_file_path", inputEvent).asString
                    val messageUUID = JsonHelper.getValueFromJson("message_metadata.message_uuid", inputEvent).asString
                    traceId = JsonHelper.getValueFromJson("routing_metadata.trace_id", inputEvent).asString
                    spanId = JsonHelper.getValueFromJson("routing_metadata.trace_id", inputEvent).asString

                    logger.info("DEX::Processing messageUUID:$messageUUID")
                    try {
                        fnConfig.psURL?.let  {
                            childSpanId =  psClientUtility.sendTraceToProcessingStatus(
                                fnConfig.psURL,
                                traceId,
                                spanId,
                                "startSpan",
                                HL7JSONLakeStageMetadata.PROCESS_NAME
                            )
                            logger.info("Span ID of messageUUID: $messageUUID : ${childSpanId}")
                        }

                        val hl7message = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
                        val fullHL7WithNulls = buildJson(hl7message)
                        // remove nulls
                        val fullHL7 = gsonNoNulls.toJsonTree(fullHL7WithNulls).asJsonObject

                        returnValue = updateMetadataAndDeliver(
                            startTime, PROCESS_STATUS_OK,
                            fullHL7, eventHubMD[messageIndex], gsonWithNullsOn,
                            inputEvent, null,
                            listOf(FunctionConfig.PROFILE_FILE_PATH),
                            outList
                        )
                        context.logger.info("Processed OK for HL7 JSON Lake messageUUID: $messageUUID, filePath: $filePath")
                    } catch (e: Exception) {
                        context.logger.severe("Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")
                        //publishing the message  to the eventhubSendErrsName topic using EventHub
                        returnValue = updateMetadataAndDeliver(
                            startTime, PROCESS_STATUS_EXCEPTION,
                            null, eventHubMD[messageIndex], gsonWithNullsOn,
                            inputEvent, e,
                            listOf(FunctionConfig.PROFILE_FILE_PATH),
                            outList
                        )
                        context.logger.info("Processed ERROR for HL7 JSON Lake messageUUID: $messageUUID, filePath: $filePath")
                    } // .catch
                    finally { // closing the span for the msg
                        fnConfig.psURL?.let {
                            psClientUtility.stopTrace(
                                fnConfig.psURL,
                                traceId,
                                childSpanId,
                                HL7JSONLakeStageMetadata.PROCESS_NAME
                            )
                        }
                    }


                } catch (e: Exception) {
                    // message is bad, can't extract fields based on schema expected
                    context.logger.severe("Unable to process Message due to exception: ${e.message}")
                    returnValue = updateMetadataAndDeliver(
                        startTime, PROCESS_STATUS_EXCEPTION,
                        null, eventHubMD[messageIndex], gsonWithNullsOn,
                        JsonObject(), e,
                        listOf(FunctionConfig.PROFILE_FILE_PATH),
                        outList
                    )
                } // .catch
            } // .for

        } catch (e: Exception) {
            logger.error("An unexpected error occurred: ${e.message}")
        }

        try {
            val errors = fnConfig.evHubSender.send(outList)
            for (i in errors) {
                val failedMessage = JsonParser.parseString(outList[i]).asJsonObject
                resendEventWithoutOutput(failedMessage)
            }
            logger.info("Send to event hub completed")
        } catch (e: Exception) {
            logger.error("Unable to send to event hub ${fnConfig.evHubSendName}: ${e.message}")
        }
        return returnValue
    }

    private fun resendEventWithoutOutput(message: JsonObject) {
        val stage = JsonHelper.getValueFromJson("metadata.stage", message).asJsonObject
        // remove output to reduce size
        stage.remove("output")
        // change stage status to Error
        stage.addProperty("status", PROCESS_STATUS_EXCEPTION)
        // change summary status to Error
        val summary = message["summary"].asJsonObject
        summary.addProperty("current_status", SUMMARY_STATUS_ERROR)
        // add problem description
        val msgId = message["message_uuid"].asString
        val uploadId = JsonHelper.getValueFromJson("routing_metadata.upload_id", message).asString
        val description = "DELIVERY FAILED: Message too large. Message UUID $msgId, Upload ID $uploadId"
        summary.add(
            "problem",
            Problem(processName = HL7JSONLakeStageMetadata.PROCESS_NAME,
                errorMessage = description).toJsonElement()
        )
        logger.error(description)
        logger.info("Retrying send without output for message UUID $msgId")
        val msg = gsonWithNullsOn.toJson(message)
        try {
            val errors = fnConfig.evHubSender.send(msg)
            if (errors.isEmpty()) {
                logger.info("Second attempt successful for message UUID $msgId")
            } else {
                // not much else we can do
                logger.error("SECOND ATTEMPT FAILED for message UUID $msgId. Message still too large.")
            }
        } catch (e: Exception) {
            logger.error("Unable to send to event hub ${fnConfig.evHubSendName}: ${e.message}")
        }

    }

    private fun updateMetadataAndDeliver(
        startTime: String, status: String,
        report: JsonObject?, eventHubMD: EventHubMetadata, gsonWithNullsOn: Gson,
        inputEvent: JsonObject, exception: Exception?,
        config: List<String>,
        outData: MutableList<String>
    ): JsonObject {

        val processMD = HL7JSONLakeStageMetadata(jsonLakeStatus = status, output = report, eventHubMD = eventHubMD, config)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()
        inputEvent.add("stage", processMD.toJsonElement())

        if (exception != null) {
            //TODO::  - update retry counts
            val problem = Problem(HL7JSONLakeStageMetadata.PROCESS_NAME, exception, false, 0, 0)
            val summary = SummaryInfo(SUMMARY_STATUS_ERROR, problem)
            inputEvent.add("summary", summary.toJsonElement())
        } else {
            inputEvent.add("summary", (SummaryInfo(SUMMARY_STATUS_OK, null).toJsonElement()))
        }
        // remove hl7 content node
        inputEvent.remove("content")
        outData.add(gsonWithNullsOn.toJson(inputEvent))
        return inputEvent
    }

    @FunctionName("transform")
    fun invoke(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>
    ): HttpResponseMessage {
        logger.info("Received HL7 message for transformation")
        val hl7Message = try {
            request.body?.get().toString()
        } catch (e: NoSuchElementException) {
            logger.error("Missing HL7 message in the request body. Caught exception: ${e.message}")
            return buildHttpResponse(
                "No body was found. Please send an HL7 v.2.x message in the body of the request.",
                HttpStatus.BAD_REQUEST,
                request
            )
        }

        return try {
            val fullHL7 = buildJson(hl7Message)
            logger.info("HL7 message has been transformed to JSONObject")
            buildHttpResponse(gsonNoNulls.toJson(fullHL7), HttpStatus.OK, request)
        } catch (e: Exception) {
            logger.error("Unable to process message due to exception: ${e.message}")
            buildHttpResponse(
                "${e.message}",
                HttpStatus.BAD_REQUEST,
                request
            )
        }
    }

} // .Function

private fun buildHttpResponse(
    message: String,
    status: HttpStatus,
    request: HttpRequestMessage<Optional<String>>
): HttpResponseMessage {
    // need to be able to send plain text exception message that is not formatted as json
    val contentType = if (status == HttpStatus.OK) {
        "application/json"
    } else {
        "text/plain"
    }
    return request
        .createResponseBuilder(status)
        .header("Content-Type", contentType)
        .body(message)
        .build()
}

private fun buildJson(message: String): JsonObject {
    val bumblebee =
        HL7JsonTransformer.getTransformerWithResource(message, FunctionConfig.PROFILE_FILE_PATH)
    return bumblebee.transformMessage()
}

