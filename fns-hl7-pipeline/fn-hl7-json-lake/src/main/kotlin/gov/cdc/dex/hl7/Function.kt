package gov.cdc.dex.hl7

import com.fasterxml.jackson.databind.JsonMappingException
import com.google.gson.*
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.bumblebee.HL7JsonTransformer
import java.util.*
import com.google.gson.JsonObject
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import org.slf4j.LoggerFactory


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
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        val fnConfig = FunctionConfig()
    } // .companion object

    @FunctionName("HL7_JSON_LAKE_TRANSFORMER")
    fun eventHubProcessor(
            @EventHubTrigger(
                    name = "msg",
                    eventHubName = "%EventHubReceiveName%",
                    consumerGroup = "%EventHubConsumerGroup%",
                    connection = "EventHubConnectionString")
            messages: List<String>?,
            @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
            context:ExecutionContext): JsonObject {

        logger.info("DEX::${context.functionName}")
        if ( messages == null) {
            return JsonObject()
        }
        val outList = mutableListOf<String>()
        var returnValue = JsonObject()
        try {
            messages.forEachIndexed { messageIndex: Int, singleMessage: String? ->
                val startTime = Date().toIsoString()
                try {
                    val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                    val metadata = JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject
                    val filePath = JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                    val messageUUID = inputEvent["message_uuid"].asString

                    logger.info("DEX::Processing messageUUID:$messageUUID")
                    try {
                        val hl7message = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
                        val fullHL7 = buildJson(hl7message)
                        returnValue = updateMetadataAndDeliver(
                            startTime, metadata, PROCESS_STATUS_OK,
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
                            startTime, metadata, PROCESS_STATUS_EXCEPTION,
                            null, eventHubMD[messageIndex], gsonWithNullsOn,
                            inputEvent, e,
                            listOf(FunctionConfig.PROFILE_FILE_PATH),
                            outList
                        )
                        context.logger.info("Processed ERROR for HL7 JSON Lake messageUUID: $messageUUID, filePath: $filePath")
                    } // .catch

                } catch (e: Exception) {
                    // message is bad, can't extract fields based on schema expected
                    context.logger.severe("Unable to process Message due to exception: ${e.message}")
                    returnValue = updateMetadataAndDeliver(
                            startTime, JsonObject(), PROCESS_STATUS_EXCEPTION,
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
            fnConfig.evHubSender.send(fnConfig.evHubSendName, outList)
        } catch (e : Exception) {
            logger.error("Unable to send to event hub ${fnConfig.evHubSendName}: ${e.message}")
        }
        return returnValue
    }


    private fun updateMetadataAndDeliver(
            startTime: String, metadata: JsonObject, status: String,
            report: JsonObject?, eventHubMD: EventHubMetadata, gsonWithNullsOn: Gson,
            inputEvent: JsonObject, exception: Exception?,
            config: List<String>,
            outData: MutableList<String>) : JsonObject {

        val processMD = HL7JSONLakeProcessMetadata(status=status, report=report, eventHubMD = eventHubMD, config)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()
        metadata.addArrayElement("processes", processMD)

        if (exception != null) {
            //TODO::  - update retry counts
            val problem = Problem(HL7JSONLakeProcessMetadata.PROCESS_NAME, exception, false, 0, 0)
            val summary = SummaryInfo(SUMMARY_STATUS_ERROR, problem)
            inputEvent.add("summary", summary.toJsonElement())
        } else {
            inputEvent.add("summary", (SummaryInfo(SUMMARY_STATUS_OK, null).toJsonElement()))
        }
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
            buildHttpResponse(gsonWithNullsOn.toJson(fullHL7), HttpStatus.OK, request)
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

private fun buildHttpResponse(message:String, status: HttpStatus, request: HttpRequestMessage<Optional<String>>) : HttpResponseMessage {
    // need to be able to send plain text exception message that is not formatted as json
    val contentType = if (status == HttpStatus.OK) { "application/json" } else { "text/plain" }
    return request
        .createResponseBuilder(status)
        .header("Content-Type", contentType)
        .body(message)
        .build()
}

private fun buildJson(message:String) : JsonObject{
    val bumblebee =
        HL7JsonTransformer.getTransformerWithResource(message, FunctionConfig.PROFILE_FILE_PATH)
    return bumblebee.transformMessage()
}

