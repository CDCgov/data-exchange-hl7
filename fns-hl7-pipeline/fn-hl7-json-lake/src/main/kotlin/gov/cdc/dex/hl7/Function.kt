package gov.cdc.dex.hl7

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


private data class OutData(
        val ok: MutableList<String> = mutableListOf(),
        val err: MutableList<String> = mutableListOf(),
        val cosmo: MutableList<JsonObject> = mutableListOf()
)
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
            context:ExecutionContext,
            @EventHubOutput(name="jsonlakeOk",
                    eventHubName = "%EventHubSendOkName%",
                    connection = "EventHubConnectionString")
            jsonlakeOkOutput : OutputBinding<List<String>>,
            @EventHubOutput(name="jsonlakeErr",
                    eventHubName = "%EventHubSendErrsName%",
                    connection = "EventHubConnectionString")
            jsonlakeErrOutput: OutputBinding<List<String>>,
            @CosmosDBOutput(name="cosmosdevpublic",
                    connection = "CosmosDBConnectionString",
                    containerName = "hl7-json-lake", createIfNotExists = true,
                    partitionKey = "/message_uuid", databaseName = "hl7-events")
            cosmosOutput: OutputBinding<List<JsonObject>> ): JsonObject {

        logger.info("DEX::${context.functionName}")
        if ( messages == null) {
            return JsonObject()
        }
        val outData = OutData()
        try {
            messages.forEachIndexed { messageIndex: Int, singleMessage: String? ->
                val startTime = Date().toIsoString()
                try {
                    val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                    val metadata = JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject
                    val filePath = JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                    val messageUUID = inputEvent["message_uuid"].asString

                    // set id messageUUID, if missing
                    if (!inputEvent.has("id")) {
                        inputEvent.add("id", messageUUID.toJsonElement())
                    }

                    logger.info("DEX::Processing messageUUID:$messageUUID")
                    try {
                        val hl7message = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
                        val fullHL7 = buildJson(hl7message, FunctionConfig.PROFILE_FILE_PATH)
                        updateMetadataAndDeliver(
                            startTime, metadata, PROCESS_STATUS_OK,
                            fullHL7, eventHubMD[messageIndex], gsonWithNullsOn,
                            inputEvent, null,
                            listOf(FunctionConfig.PROFILE_FILE_PATH),
                            outData
                        )
                        context.logger.info("Processed OK for HL7 JSON Lake messageUUID: $messageUUID, filePath: $filePath")
                        if (messageIndex == messages.lastIndex) {
                            return inputEvent
                        }
                    } catch (e: Exception) {
                        context.logger.severe("Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")
                        //publishing the message  to the eventhubSendErrsName topic using EventHub
                        updateMetadataAndDeliver(
                            startTime, metadata, PROCESS_STATUS_EXCEPTION,
                            null, eventHubMD[messageIndex], gsonWithNullsOn,
                            inputEvent, e,
                            listOf(FunctionConfig.PROFILE_FILE_PATH),
                            outData
                        )
                        context.logger.info("Processed ERROR for HL7 JSON Lake messageUUID: $messageUUID, filePath: $filePath")
                        return inputEvent
                    } // .catch

                } catch (e: Exception) {
                    // message is bad, can't extract fields based on schema expected
                    context.logger.severe("Unable to process Message due to exception: ${e.message}")
                    updateMetadataAndDeliver(
                            startTime, JsonObject(), PROCESS_STATUS_EXCEPTION,
                            null, eventHubMD[messageIndex], gsonWithNullsOn,
                            JsonObject(), e,
                            listOf(FunctionConfig.PROFILE_FILE_PATH),
                            outData
                    )
                    return JsonObject()
                } // .catch
            }
            return JsonObject()
        } finally {
            with(outData) {
                jsonlakeOkOutput.value = ok
                jsonlakeErrOutput.value = err
                cosmosOutput.value = cosmo
            }
        }
    }

    private fun updateMetadataAndDeliver(
            startTime: String, metadata: JsonObject, status: String,
            report: JsonObject?, eventHubMD: EventHubMetadata, gsonWithNullsOn: Gson,
            inputEvent: JsonObject, exception: Exception?,
            config: List<String>,
            outData:OutData) {

        val processMD = HL7JSONLakeProcessMetadata(status=status, report=report, eventHubMD = eventHubMD, config)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()
        metadata.addArrayElement("processes", processMD)

        if (exception != null) {
            //TODO::  - update retry counts
            val problem = Problem(HL7JSONLakeProcessMetadata.PROCESS_NAME, exception, false, 0, 0)
            val summary = SummaryInfo(SUMMARY_STATUS_ERROR, problem)
            inputEvent.add("summary", summary.toJsonElement())
            outData.err.add(gsonWithNullsOn.toJson(inputEvent))
        } else {
            inputEvent.add("summary", (SummaryInfo(SUMMARY_STATUS_OK, null).toJsonElement()))
            outData.ok.add(gsonWithNullsOn.toJson(inputEvent))
        }
        outData.cosmo.add((gsonWithNullsOn.toJsonTree(inputEvent) as JsonObject))
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
            val fullHL7 = buildJson(hl7Message, FunctionConfig.PROFILE_FILE_PATH)
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

private fun buildJson(message:String, profilePath : String) : JsonObject{
    val bumblebee =
        HL7JsonTransformer.getTransformerWithResource(message, profilePath)
    return bumblebee.transformMessage()
}

