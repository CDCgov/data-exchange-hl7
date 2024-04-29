package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.model.RedactorReport
import gov.cdc.dex.hl7.model.RedactorStageMetadata
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Azure function with event hub trigger to redact messages   */
class Function {

    companion object {
        val gson: Gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        val fnConfig = FunctionConfig()
    }

    @FunctionName("redactor")
    fun eventHubProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnection",
            consumerGroup = "%EventHubConsumerGroup%",
        )
        message: List<String?>,
        @BindingName("SystemPropertiesArray") eventHubMD: List<EventHubMetadata>,
    ): JsonObject {
        val helper = Helper()
        val outList = mutableListOf<String>()
        var inputEvent = JsonObject()

        try {
            message.forEachIndexed { msgIndex: Int, singleMessage: String? ->
                // context.logger.info("------ singleMessage: ------>: --> $singleMessage")
                val startTime = Date().toIsoString()
                inputEvent = JsonParser.parseString(singleMessage) as JsonObject
                val hl7Content: String
                val filePath: String
                val messageUUID: String

                try {
                    // Extract from event
                    hl7Content = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)

                    filePath = JsonHelper.getValueFromJson("routing_metadata.ingested_file_path", inputEvent).asString
                    messageUUID = JsonHelper.getValueFromJson("message_metadata.message_uuid", inputEvent).asString

                    val dataStreamId = JsonHelper.getValueFromJson("routing_metadata.data_stream_id", inputEvent).asString

                    logger.info("DEX:: Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                    val configFileName = helper.getConfigFileName(hl7Content = hl7Content,
                        profileConfig = fnConfig.profileConfig, dataStreamId= dataStreamId)

                    val report = helper.getRedactedReport(msg=hl7Content, configFileName= configFileName)

                    if (report != null) {
                        val rReport = RedactorReport(report._2())
                        val stageMD = RedactorStageMetadata(
                            redactorStatus = rReport.status,
                            report = rReport,
                            eventHubMetadata = eventHubMD[msgIndex],
                            config = listOf(configFileName)
                        )
                        stageMD.startProcessTime = startTime
                        stageMD.endProcessTime = Date().toIsoString()
                        logger.info("Process MD: $stageMD ")


                        inputEvent.add("stage", stageMD.toJsonElement())
                        val newContentBase64 =
                            Base64.getEncoder().encodeToString((report._1()?.toByteArray() ?: "") as ByteArray?)
                        inputEvent.add("content", JsonParser.parseString(gson.toJson(newContentBase64)))
                        //Update Summary element.
                        val summary = SummaryInfo("REDACTED")
                        inputEvent.add("summary", JsonParser.parseString(gson.toJson(summary)))
                        logger.info("DEX:: Handled Redaction for messageUUID: $messageUUID, filePath: $filePath, ehDestination: ${fnConfig.evHubSendName}")
                        outList.add(gson.toJson(inputEvent))
                    }
                } catch (e: Exception) {
                    //TODO::  - update retry counts
                    logger.error("DEX:: Unable to process Message due to exception: ${e.message}")
                    val problem = Problem(RedactorStageMetadata.REDACTOR_PROCESS, e, false, 0, 0)

                    val summary = SummaryInfo("FAILURE", problem)
                    inputEvent.add("summary", summary.toJsonElement())
                    outList.add(gson.toJson(inputEvent))
                }

            } // .eventHubProcessor
        } catch (ex: Exception) {
            logger.error("DEX:error occurred: ${ex.message}")
        }
        try {
            val errors = fnConfig.evHubSender.send(outList)
            if (errors.isNotEmpty()) {
                // one or more messages could not be delivered because they are too large
                errors.forEach { errIndex ->
                    val erredMessage = JsonParser.parseString(outList[errIndex]).asJsonObject
                    val id = JsonHelper.getValueFromJson("message_metadata.message_uuid", erredMessage).asString
                    logger.error(
                        "DEX::ERROR: Event with messageUUID $id could not be delivered" +
                                " because it is too large"
                    )
                }
                throw Exception("Could not deliver ${errors.size} messages due to excessive size")
            }
        } catch (e: Exception) {
            logger.error("Unable to send to event hub ${fnConfig.evHubSendName}: ${e.message}")
            throw e
        }
        return inputEvent
    }

    @FunctionName("redactorReport")
    fun invoke(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext
    ): HttpResponseMessage {

        val hl7Message: String?
        val helper = Helper()
        try {
            hl7Message = request.body?.get().toString()
        } catch (e: NoSuchElementException) {
            return noBodyResponse(request)
        }

        return try {
            val dataStreamId: String = request.headers["x-tp-data_stream_id"] ?: ""

            if (dataStreamId.isEmpty()) {
                return buildHttpResponse(
                    "Error: Data Stream ID must be specified in message header using key 'x-tp-data_stream_id'.",
                    HttpStatus.BAD_REQUEST,
                    request
                )
            }
            val configFileName = helper.getConfigFileName(hl7Content = hl7Message,
                profileConfig = fnConfig.profileConfig, dataStreamId= dataStreamId)
            val report = helper.getRedactedReport(hl7Message, configFileName)

            buildHttpResponse(gson.toJson(report), HttpStatus.OK, request)
        } catch (e: Exception) {
            buildHttpResponse(
                "Error: ${e.message}",
                HttpStatus.BAD_REQUEST,
                request
            )
        }
    }
}

private fun noBodyResponse(request: HttpRequestMessage<Optional<String>>): HttpResponseMessage {
    return buildHttpResponse(
        "No body was found. Please send an HL7 v.2.x message in the body of the request.",
        HttpStatus.BAD_REQUEST,
        request
    )
}

private fun buildHttpResponse(
    message: String,
    status: HttpStatus,
    request: HttpRequestMessage<Optional<String>>
): HttpResponseMessage {

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