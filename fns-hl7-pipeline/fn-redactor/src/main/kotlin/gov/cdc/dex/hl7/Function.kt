package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.model.RedactorProcessMetadata
import gov.cdc.dex.hl7.model.RedactorReport
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import java.util.*
import org.slf4j.LoggerFactory

/**
 * Azure function with event hub trigger to redact messages   */
class Function {

    companion object {
        val gson: Gson = GsonBuilder().serializeNulls().create()
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        val fnConfig = FunctionConfig()
    }
    @FunctionName("Redactor")
    fun eventHubProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%",
        )
        message: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        @EventHubOutput(name="redactorOk",
            eventHubName = "%EventHubSendOkName%",
            connection = "EventHubConnectionString") redactorOkOutput : OutputBinding<List<String>>,
        @EventHubOutput(name="redactorErr",
            eventHubName = "%EventHubSendErrsName%",
            connection = "EventHubConnectionString") redactorErrOutput: OutputBinding<List<String>>,
        @CosmosDBOutput(name="cosmosdevpublic",
            connection = "CosmosDBConnectionString",
            containerName = "hl7-redactor", createIfNotExists = true,
            partitionKey = "/message_uuid", databaseName = "hl7-events") cosmosOutput: OutputBinding<List<JsonObject>>,
        context: ExecutionContext
    ): JsonObject {
        val helper = Helper()
        val outOkList = mutableListOf<String>()
        val outErrList = mutableListOf<String>()
        val outEventList = mutableListOf<JsonObject>()

        message.forEachIndexed { msgIndex: Int, singleMessage: String? ->
           // context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            val startTime = Date().toIsoString()
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            val hl7Content : String
            val metadata : JsonObject
            val filePath : String
            val messageUUID : String

            try {
                // Extract from event
                hl7Content = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
                metadata = JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject

                filePath = JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString

                val messageType = JsonHelper.getValueFromJson("message_info.type", inputEvent).asString
                val routeElement = JsonHelper.getValueFromJson("message_info.route", inputEvent)
                val route = if (routeElement.isJsonNull) {
                    ""
                } else {
                    routeElement.asString
                }

				logger.info("DEX:: Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                val report = helper.getRedactedReport(hl7Content, messageType, route)

                if(report != null) {
                    val rReport = RedactorReport(report._2())
                    val configFileName : List<String> = listOf(helper.getConfigFileName(messageType, route))
                    val processMD = RedactorProcessMetadata(rReport.status, report = rReport, eventHubMD[msgIndex], configFileName)
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()
                    logger.info("Process MD: ${processMD} ")

                    metadata.addArrayElement("processes", processMD)
                    val newContentBase64 = Base64.getEncoder().encodeToString((report._1()?.toByteArray() ?: "") as ByteArray?)
                    inputEvent.add("content", JsonParser.parseString(gson.toJson(newContentBase64)))
                    //Update Summary element.
                    val summary = SummaryInfo("REDACTED")
                    inputEvent.add("summary", JsonParser.parseString(gson.toJson(summary)))
                    logger.info("DEX:: Handled Redaction for messageUUID: $messageUUID, filePath: $filePath, ehDestination: $fnConfig.evHubOkName")
                    outOkList.add(gson.toJson(inputEvent))
                    outEventList.add(gson.toJsonTree(inputEvent) as JsonObject)
                    //fnConfig.evHubSender.send(fnConfig.evHubOkName, gson.toJson(inputEvent))
                    if (msgIndex == message.lastIndex){
                        return inputEvent
                    }
                }
                if (msgIndex == message.lastIndex){
                    return inputEvent
                }
            } catch (e: Exception) {
                //TODO::  - update retry counts
                logger.error("DEX:: Unable to process Message due to exception: ${e.message}")
                val problem = Problem(RedactorProcessMetadata.REDACTOR_PROCESS, e, false, 0, 0)

                val summary = SummaryInfo("FAILURE", problem)
                inputEvent.add("summary", summary.toJsonElement())
                outErrList.add(gson.toJson(inputEvent))
                outEventList.add(gson.toJsonTree(inputEvent) as JsonObject)
                //fnConfig.evHubSender.send(fnConfig.evHubErrorName, gson.toJson(inputEvent))
                return inputEvent
            }
            redactorOkOutput.value = outOkList.toList()
            redactorErrOutput.value = outErrList.toList()
            cosmosOutput.value = outEventList.toList()
        } // .eventHubProcessor
        return JsonObject()
    }

    @FunctionName("redactorReport")
    fun invoke(
        @HttpTrigger(name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS)
        request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext): HttpResponseMessage {

        val hl7Message : String?
        val helper = Helper()
        try {
            hl7Message = request.body?.get().toString()
        } catch (e: NoSuchElementException) {
            return noBodyResponse(request)
        }

        return try {
            val messageType: String = request.headers["x-tp-message_type"] ?: ""
            val route: String = request.headers["x-tp-route"] ?: ""
            if (messageType.isEmpty()) {
                return buildHttpResponse(
                    "Error: Message type (CASE or ELR) must be specified in " +
                            "message header using key 'x-tp-message_type'.",
                    HttpStatus.BAD_REQUEST,
                    request
                )
            }
            if (messageType.uppercase() == "ELR" && route.isEmpty()) {
                return buildHttpResponse(
                    "Error: Route must be specified in message header using key 'x-tp-route'.",
                    HttpStatus.BAD_REQUEST,
                    request
                )
            }

            val report = helper.getRedactedReport(hl7Message, messageType, route)

            buildHttpResponse(gson.toJson(report), HttpStatus.OK, request)
        } catch (e: Exception) {
            noBodyResponse(request)
        }
    }

}

private fun noBodyResponse(request: HttpRequestMessage<Optional<String>>) : HttpResponseMessage {
    return buildHttpResponse(
        "No body was found. Please send an HL7 v.2.x message in the body of the request.",
        HttpStatus.BAD_REQUEST,
        request
    )
}

private fun buildHttpResponse(message:String, status: HttpStatus, request: HttpRequestMessage<Optional<String>>) : HttpResponseMessage {
    var contentType = "application/json"
    if (status != HttpStatus.OK) {
        contentType = "text/plain"
    }
    return request
        .createResponseBuilder(status)
        .header("Content-Type", contentType)
        .body(message)
        .build()
}