package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import java.util.*

import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.metadata.*
import gov.cdc.dex.util.DateHelper.toIsoString

import gov.cdc.dex.hl7.model.ReportStatus

import gov.cdc.dex.metadata.Problem

/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    
    companion object {
        private const val MMG_VALIDATOR = "MMG-VALIDATOR"
        private const val STATUS_ERROR = "ERROR"
    } // .companion

    @FunctionName("mmgvalidator001")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveName%",
                connection = "EventHubConnectionString",
                consumerGroup = "%EventHubConsumerGroup%",) 
                message: List<String?>,
                context: ExecutionContext) {


        val startTime =  Date().toIsoString()
        // context.logger.info("received event: --> $message") 

        // set up the 2 out event hubs, TODO: change to library
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val eventHubSendOkName = System.getenv("EventHubSendOkName")
        val eventHubSendErrsName = System.getenv("EventHubSendErrsName")

        val evHubSender = EventHubSender(evHubConnStr)
        val ehSender = EventHubSender(evHubConnStr)

        message.forEach { singleMessage: String? ->
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            // context.logger.info("singleMessage: --> $singleMessage")

            try {
                val hl7ContentBase64 = inputEvent["content"].asString

                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = String(hl7ContentDecodedBytes)

                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject

                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString
                
                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")
    
                try {
                    // get MMG(s) for the message:
                    val mmgs = MmgUtil.getMMGFromMessage(hl7Content, filePath, messageUUID)
                    // mmgs.forEach {
                    //     context.logger.info("MMG blocks found for messageUUID: $messageUUID, filePath: $filePath, BLOCKS: --> ${it.blocks.size}")
                    // }

                    val mmgValidator = MmgValidator( hl7Content, mmgs )
                    val validationReport = mmgValidator.validate()

                    val otherSegmentsValidator = MmgValidatorOtherSegments( hl7Content, mmgs )
                    val validationReportOtherSegments = otherSegmentsValidator.validate()

                    val validationReportFull = validationReport + validationReportOtherSegments
                    context.logger.info("MMG Validation Report size for for messageUUID: $messageUUID, filePath: $filePath, size --> " + validationReportFull.size)

                    // adding the content validation report to received message 
                    // and sending to next event hub

                    // get report status 
                    var reportStatus = MmgReport(validationReportFull).getReportStatus()
                    

                    val processMD = MmgValidatorProcessMetadata(reportStatus.toString(), validationReportFull)
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()

                    inputEvent.addArrayElement("processes", processMD)
                    //TODO:: Update Summary element.

                    // context.logger.info("INPUT EVENT OUT: --> ${inputEvent}")

                    //Send event
                    val ehDestination = if (reportStatus == ReportStatus.MMG_ERRORS) eventHubSendErrsName else eventHubSendOkName
                    evHubSender.send(evHubTopicName=ehDestination, message=Gson().toJson(inputEvent))
                    context.logger.info("Processed for MMG validated messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination, reportStatus: ${reportStatus}")

                } catch (e: Exception) {
                    context.logger.severe("Unable to process Message due to exception: ${e.message}")

                    val problem = Problem(MMG_VALIDATOR, e, false, 0, 0)
                    val summary = SummaryInfo(STATUS_ERROR, problem)
                    inputEvent.add("summary", summary.toJsonElement())

                    evHubSender.send( evHubTopicName=eventHubSendErrsName, message=Gson().toJson(inputEvent) )
                    // throw  Exception("Unable to process Message messageUUID: $messageUUID, filePath: $filePath due to exception: ${e.message}")
                } 

            } catch (e: Exception) {
                //TODO::  - update retry counts
                context.logger.severe("Unable to process Message due to exception: ${e.message}")

                val problem = Problem(MMG_VALIDATOR, e, false, 0, 0)
                val summary = SummaryInfo(STATUS_ERROR, problem)
                inputEvent.add("summary", summary.toJsonElement())

                evHubSender.send( evHubTopicName=eventHubSendErrsName, message=Gson().toJson(inputEvent) )
                // e.printStackTrace()
            }
        } // .message.forEach


    } // .eventHubProcessor

} // .Function

