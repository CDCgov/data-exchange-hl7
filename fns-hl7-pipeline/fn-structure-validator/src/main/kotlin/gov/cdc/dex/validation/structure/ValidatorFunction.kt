package gov.cdc.dex.validation.structure

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubSender

import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.model.StructureValidatorProcessMetadata
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.nist.validator.InvalidFileException

import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.slf4j.LoggerFactory
import java.util.*


class ValidatorFunction {
    companion object {
        private const val PHIN_SPEC_PROFILE = "MSH-21[1].1" //Not able to use HL7-PET due to scala version conflicts with NistValidator.
        private const val NIST_VALID_MESSAGE = "VALID_MESSAGE"

        private val logger = LoggerFactory.getLogger(ValidatorFunction::class.java.simpleName)
        val gson = Gson()
    }
    /**
     * This function will be invoked when an event is received from Event Hub.
     */
    @FunctionName("NIST-VALIDATOR")
    fun run(
        @EventHubTrigger(
            name = "message",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%",
            // cardinality = Cardinality.MANY
        ) message: List<String?>,
        context: ExecutionContext
    ) {
        context.logger.info("Event received message.size: ${message.size}")

        val startTime =  Date().toIsoString()

        val evHubNameOk = System.getenv("EventHubSendOkName")
        val evHubNameErrs = System.getenv("EventHubSendErrsName")
        val evHubConnStr = System.getenv("EventHubConnectionString")

        val ehSender = EventHubSender(evHubConnStr)

        message.forEach { singleMessage: String? ->
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            // context.logger.info("singleMessage: --> $singleMessage")

            try {
                val hl7Content = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
                val metadata = JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject

                val filePath = JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                val messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString
                
                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                var phinSpec: String? = null
                try {
                    phinSpec = hl7Content.split("\n")[0].split("|")[20].split("^")[0]
                    context.logger.fine("Processing Structure Validation for profile $phinSpec")
                    val nistValidator = ProfileManager(ResourceFileFetcher(), "/${phinSpec.uppercase()}")
                    val report = nistValidator.validate(hl7Content)
                    val processMD = StructureValidatorProcessMetadata(report.status?: "Unknown", report)
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()

                    metadata.addArrayElement("processes", processMD)
                    //Update Summary element.
                    val summary = SummaryInfo(report.status ?: "Unknown")
                    if (NIST_VALID_MESSAGE != report.status ) {
                        summary.problem= Problem(StructureValidatorProcessMetadata.VALIDATOR_PROCESS, "Message failed Structure Validation" )
                    }
                    inputEvent.add("summary", JsonParser.parseString(gson.toJson(summary)))
                    //Send event
                    val ehDestination = if (NIST_VALID_MESSAGE == report.status) evHubNameOk else evHubNameErrs


                    context.logger.info("Processed structure validation for messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination, report.status: ${report.status}")
                    context.logger.finest("INPUT EVENT OUT: --> ${ gson.toJson(inputEvent) }")
                    ehSender.send(ehDestination, Gson().toJson(inputEvent))
                } catch (e: ArrayIndexOutOfBoundsException) {
                    throw  InvalidMessageException("Unable to process Message: Unable to retrieve Phin Specification from message, MSH-21[1].1 Not found - messageUUID: $messageUUID, filePath: $filePath. ")
                } catch (e: InvalidFileException) {
                    throw InvalidMessageException("Unable to process Message due to Phin Spec: ${phinSpec} not recognized.  messageUUID: $messageUUID, filePath: $filePath")
                }

            } catch (e: Exception) {
                //TODO::  - update retry counts
                context.logger.severe("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()
                val problem = Problem(StructureValidatorProcessMetadata.VALIDATOR_PROCESS, e, false, 0, 0)
                val summary = SummaryInfo("STRUCTURE_ERROR", problem)
                inputEvent.add("summary", summary.toJsonElement())
                ehSender.send(evHubNameErrs, Gson().toJson(inputEvent))
            }
        }
    }
}