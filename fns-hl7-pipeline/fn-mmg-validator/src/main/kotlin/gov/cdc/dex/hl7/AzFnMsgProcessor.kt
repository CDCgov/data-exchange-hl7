package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.hl7.model.ValidationReport
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import java.util.*

abstract class AzFnMsgProcessor {
    companion object {
        const val STATUS_ERROR = "ERROR"
    }
    fun exec(message: List<String?>, context: ExecutionContext) {
        val startTime = Date().toIsoString()
        // context.logger.info("received event: --> $message")
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val eventHubSendOkName = System.getenv("EventHubSendOkName")
        val eventHubSendErrsName = System.getenv("EventHubSendErrsName")

        val evHubSender = EventHubSender(evHubConnStr)
        val gson = Gson()
        message.forEach { singleMessage: String? ->
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            try {
                val metadata = inputEvent["metadata"].asJsonObject
//                val hl7ContentBase64 = JsonHelper.getValueFromJson("content", inputEvent).asString
//                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)

                val processResult = validateMessage(hl7Content)

                 //Add Process MD
                addProcessStatus(startTime, metadata, processResult.status, processResult)
                //Prepare Summary:
                prepareSummary(processResult.status, inputEvent)
                //Send event

                context.logger.info("INPUT EVENT OUT: --> ${gson.toJson(inputEvent)}")

                val ehDestination =
                    if (processResult.status == ValidationReport.VALID_MESSAGE) eventHubSendOkName else eventHubSendErrsName
                evHubSender.send(evHubTopicName = ehDestination, message = gson.toJson(inputEvent))

                val filePath = JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                val messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString
                context.logger.info("Process messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination, reportStatus: ${processResult.status}")
            } catch (e: Exception) {
                context.logger.severe("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()
                val problem = Problem(getProcessName(), e, false, 0, 0)
                val summary = SummaryInfo(STATUS_ERROR, problem)
                inputEvent.add("summary", summary.toJsonElement())

                evHubSender.send(evHubTopicName = eventHubSendErrsName, message = Gson().toJson(inputEvent))
            }
        }
    }

    abstract fun getProcessName(): String
    abstract fun prepareSummary(status: String, inputEvent: JsonObject)
    abstract fun addProcessStatus(startTime: String, metadata: JsonObject, status: String, report: ValidationReport)
    abstract fun validateMessage(hl7Content: String): ValidationReport
}