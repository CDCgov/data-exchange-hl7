package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.hl7.model.RedactorProcessMetadata
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.gson
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.DeIdentifier
import java.util.*


/**
 * Azure function with event hub trigger to redact messages   */
class Function {


    @FunctionName("Redactor")
    fun eventHubProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%",
        )
        message: List<String?>,
        context: ExecutionContext
    ) {
        context.logger.info("------ received event: ------> message: --> $message")

        val REDACTOR_STATUS_OK = "PROCESS_REDACTOR_OK"
        val startTime = Date().toIsoString()
        val evHubNameOk = System.getenv("EventHubSendOkName")
        val evHubNameErrs = System.getenv("EventHubSendErrsName")
        val evHubConnStr = System.getenv("EventHubConnectionString")

        val ehSender = EventHubSender(evHubConnStr)

        message.forEach { singleMessage: String? ->
            context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            try {
                // Extract from event
                var hl7ContentBase64 = inputEvent["content"].asString
                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = String(hl7ContentDecodedBytes)
                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject
                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString

                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                var d = DeIdentifier()
                var helper = Helper()
                val report = helper.getRedactedReport(hl7Content)
                val processMD = RedactorProcessMetadata(status = REDACTOR_STATUS_OK, report = report?._2()?.toList())
                processMD.startProcessTime = startTime
                processMD.endProcessTime = Date().toIsoString()

                metadata.addArrayElement("processes", processMD)

                val newContentBase64 =
                    Base64.getEncoder().encodeToString((report?._1()?.toByteArray() ?: "") as ByteArray?)
                inputEvent.add("content", JsonParser.parseString(gson.toJson(newContentBase64)))

                println("inputEvent new :${inputEvent}")
                context.logger.info("Handled Redaction for messageUUID: $messageUUID, filePath: $filePath, ehDestination: $evHubNameOk ")
                context.logger.finest("INPUT EVENT OUT: --> ${gson.toJson(inputEvent)}")
                ehSender.send(evHubNameOk, Gson().toJson(inputEvent))

            } catch (e: Exception) {
                //TODO::  - update retry counts
                context.logger.severe("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()
                val problem = Problem(RedactorProcessMetadata.REDACTOR_PROCESS, e, false, 0, 0)

                val summary = SummaryInfo("REDACTOR_ERROR", problem)
                inputEvent.add("summary", summary.toJsonElement())
                ehSender.send(evHubNameErrs, Gson().toJson(inputEvent))
            }


        } // .eventHubProcessor

    }


} // .Function
