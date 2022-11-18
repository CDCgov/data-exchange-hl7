package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.hl7.exception.InvalidMessageException
import gov.cdc.dex.hl7.model.MmgReport
import gov.cdc.dex.hl7.model.MmgValidatorProcessMetadata
import gov.cdc.dex.hl7.model.ReportStatus
import gov.cdc.dex.hl7.model.ValidationIssueCategoryType
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.HL7StaticParser
import java.util.*

/**
 * Azure Functions with Event Hub Trigger.
 */
class MMGValidationFunction {
    
    companion object {
        private const val STATUS_ERROR = "ERROR"

        val gson = Gson()
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
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val eventHubSendOkName = System.getenv("EventHubSendOkName")
        val eventHubSendErrsName = System.getenv("EventHubSendErrsName")

        val evHubSender = EventHubSender(evHubConnStr)
//        val ehSender = EventHubSender(evHubConnStr)

        message.forEach { singleMessage: String? ->
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            // context.logger.info("singleMessage: --> $singleMessage")

            try {
                val hl7ContentBase64 = JsonHelper.getValueFromJson("content", inputEvent).asString

                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = String(hl7ContentDecodedBytes)

                val metadata = inputEvent["metadata"].asJsonObject

                val filePath = JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                val messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString
                
                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")
    
//                try {
                    // get MMG(s) for the message:
                    //val mmgs = MmgUtil.getMMGFromMessage(hl7Content, filePath, messageUUID)
                    // mmgs.forEach {
                    //     context.logger.info("MMG blocks found for messageUUID: $messageUUID, filePath: $filePath, BLOCKS: --> ${it.blocks.size}")
                    // }

                    val mmgValidator = MmgValidator()
                    val validationReport = mmgValidator.validate(hl7Content)

                   // val otherSegmentsValidator = MmgValidatorOtherSegments( hl7Content, mmgs )
                    //val validationReportOtherSegments = otherSegmentsValidator.validateOtherSegments()

                    //val validationReportFull = validationReport + validationReportOtherSegments
                    context.logger.info("MMG Validation Report size for for messageUUID: $messageUUID, filePath: $filePath, size --> " + validationReport.size)

                    // adding the content validation report to received message 
                    // and sending to next event hub

                    // get report status
                    val errorCount = validationReport.count { it.classification == ValidationIssueCategoryType.ERROR}
                    val warningCount = validationReport.count{ it.classification == ValidationIssueCategoryType.WARNING}
                    val mmgReport = MmgReport(errorCount, warningCount, validationReport)
                    

                    val processMD = MmgValidatorProcessMetadata(mmgReport.toString(), mmgReport)
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()

                    metadata.addArrayElement("processes", processMD)
                    //Prepare Summary:
                    val summary = SummaryInfo(mmgReport.status.toString())
                    if (ReportStatus.MMG_ERRORS == mmgReport.status ) {
                        summary.problem= Problem(MmgValidatorProcessMetadata.MMG_VALIDATOR_PROCESS, null, null, "Message failed MMG Validation", false, 0, 0)
                    }
                    inputEvent.add("summary", JsonParser.parseString(gson.toJson(summary)))
                    //Send event
                    context.logger.info("INPUT EVENT OUT: --> ${ gson.toJson(inputEvent) }")

                val ehDestination = if (mmgReport.status == ReportStatus.MMG_VALID) eventHubSendOkName else eventHubSendErrsName
                    evHubSender.send(evHubTopicName=ehDestination, message=gson.toJson(inputEvent))
                    context.logger.info("Processed for MMG validated messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination, reportStatus: ${mmgReport}")

//                } catch (e: Exception) {
//                    context.logger.severe("Unable to process Message due to exception: ${e.message}")
//
//                    val problem = Problem(MMG_VALIDATOR, e, false, 0, 0)
//                    val summary = SummaryInfo(STATUS_ERROR, problem)
//                    inputEvent.add("summary", summary.toJsonElement())
//
//                    evHubSender.send( evHubTopicName=eventHubSendErrsName, message=Gson().toJson(inputEvent) )
//                    // throw  Exception("Unable to process Message messageUUID: $messageUUID, filePath: $filePath due to exception: ${e.message}")
//                }

            } catch (e: Exception) {
                //TODO::  - update retry counts
                context.logger.severe("Unable to process Message due to exception: ${e.message}")

                val problem = Problem(MmgValidatorProcessMetadata.MMG_VALIDATOR_PROCESS, e, false, 0, 0)
                val summary = SummaryInfo(STATUS_ERROR, problem)
                inputEvent.add("summary", summary.toJsonElement())

                evHubSender.send( evHubTopicName=eventHubSendErrsName, message=Gson().toJson(inputEvent) )
                // e.printStackTrace()
            }
        } // .message.forEach
    } // .eventHubProcessor

} // .Function

