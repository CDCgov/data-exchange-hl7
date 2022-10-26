package gov.cdc.dex.validation.structure

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.Cardinality
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.Problem
import gov.cdc.dex.ProcessMetadata
import gov.cdc.dex.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.EventHubSender
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.nist.validator.InvalidFileException

import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.slf4j.LoggerFactory
import java.util.*


class ValidatorFunction {
    companion object {
        private const val PHIN_SPEC_PROFILE = "MSH-21[1].1"
        private const val STRUCTURE_VALIDATOR = "STRUCTURE-VALIDATOR"
        private const val STRUCTURE_VERSION = "1.0.0"
        private const val STATUS_SUCCESS = "SUCCESS"
        private const val STATUS_ERROR = "ERROR"

        private val logger = LoggerFactory.getLogger(ValidatorFunction::class.java.simpleName)
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

        // TODO: remove log
        // context.logger.info("received event: --> $message") 

        val startTime =  Date().toIsoString()

        val evHubNameOk = System.getenv("EventHubSendOkName")
        val evHubNameErrs = System.getenv("EventHubSendErrsName")
        val evHubConnStr = System.getenv("EventHubConnectionString")

        val ehSender = EventHubSender(evHubConnStr)

        message.forEach { singleMessage: String? ->
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            try {
                val hl7Content = inputEvent["content"].asString

                val metadata = inputEvent["metadata"].asJsonObject
                val messageUUID = metadata["messageUUID"].asString
                val fileName = metadata["fileName"].asString
                context.logger.info("Received and Processing messageUUID: $messageUUID, fileName: $fileName")

                var phinSpec: String? = null
                try {

                    phinSpec = hl7Content.split("\n")[0].split("|")[20].split("^")[0]
                    context.logger.fine("Processing Structure Validation for profile $phinSpec")
                    val nistValidator = ProfileManager(ResourceFileFetcher(), "/${phinSpec.uppercase()}")
                    val report = nistValidator.validate(hl7Content)
                    val processMD = ProcessMetadata(
                        STRUCTURE_VALIDATOR, STRUCTURE_VERSION,
                        report.status
                    )
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()
                    processMD.report = report

                    inputEvent.addArrayElement("processes", processMD)
                    //TODO:: Update Summary element.

                    //Send event
                    val ehDestination = if ("STRUCTURE_VALID" == report.status) evHubNameOk else evHubNameErrs

                    ehSender.send(Gson().toJson(inputEvent), ehDestination)

                    context.logger.info("Processed for structure validated messageUUID: $messageUUID, fileName: $fileName, ehDestination: $ehDestination, report.status: ${report.status}")

                } catch (e: ArrayIndexOutOfBoundsException) {
                    throw  InvalidMessageException("Unable to process Message messageUUID: $messageUUID, fileName: $fileName. Unable to retrieve Phin Specification from message, MSH-21[1].1 Not found")
                } catch (e: InvalidFileException) {
                    throw InvalidMessageException("Unable to process Message messageUUID: $messageUUID, fileName: $fileName due to not found Phin Spec: ${phinSpec}")
                }

            } catch (e: Exception) {
                //TODO::  - update retry counts
                context.logger.severe("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()
                val problem = Problem(STRUCTURE_VALIDATOR, e, false, 0, 0)
                val summary = SummaryInfo("STRUCTURE_ERROR", problem)
                inputEvent.add("summary", summary.toJsonElement())
                ehSender.send(Gson().toJson(inputEvent), evHubNameErrs)
            }
        }
    }
}