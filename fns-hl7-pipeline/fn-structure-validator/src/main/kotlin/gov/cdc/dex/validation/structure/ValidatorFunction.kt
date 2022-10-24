package gov.cdc.dex.validation.structure

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.Cardinality
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.ProcessMetadata
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.EventHubSender
import gov.cdc.dex.util.JsonHelper.addArrayElement

import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import java.util.*


class ValidatorFunction {
    companion object {
        private const val PHIN_SPEC_PROFILE = "MSH-21[1].1"
        private const val STRUCTURE_VALIDATOR = "STRUCTURE-VALIDATOR"
        private const val STRUCTURE_VERSION = "1.0.0"
        private const val STATUS_SUCCESS = "SUCCESS"
        private const val STATUS_ERROR = "ERROR"
    }
    /**
     * This function will be invoked when an event is received from Event Hub.
     */
    @FunctionName("NIST-VALIDATOR")
    fun run(
        @EventHubTrigger(
            name = "message",
            eventHubName = "myeventhub",
            connection = "MyStorageConnectionAppSetting",
            consumerGroup = "\$Default",
            cardinality = Cardinality.MANY
        ) message: List<String?>,
        context: ExecutionContext
    ) {
        val startTime =  Date().toIsoString()

        val evHubNameOk = System.getenv("EventHubSendOkName")
        val evHubNameErrs = System.getenv("EventHubSendErrsName")
        val evHubConnStr = System.getenv("EventHubConnectionString")

        context.logger.info("Java Event Hub trigger function executed.")
        context.logger.info("Length:" + message.size)
        message.forEach { singleMessage: String? ->
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            // context.getLogger.info("Json Array size:" + elem.getAsJsonArray)

            val hl7Content = inputEvent["content"].asString

            val phinSpec =  hl7Content.split("\n")[0].split("\\|")[20].split("\\^")[0]
            val nistValidator = ProfileManager(ResourceFileFetcher(), phinSpec )
            val report = nistValidator.validate(hl7Content)
            val processMD = ProcessMetadata(
                STRUCTURE_VALIDATOR, STRUCTURE_VERSION,
                inputEvent["id"].asString, inputEvent["eventTime"].asString,
                report.status!!
            )
            processMD.startProcessTime = startTime
            processMD.endProcessTime = Date().toIsoString()
            processMD.report = report

            inputEvent.addArrayElement("processes", processMD)
            //TODO:: Update Summary element.

            //Send event
            val ehDestination = if ("STRUCTURE_VALID" == report.status) evHubNameOk else evHubNameErrs
            val sender = EventHubSender(evHubConnStr)
            sender.send(Gson().toJson(inputEvent), ehDestination)

        }
    }

//     fun addArrayToEvent(inputEvent: JsonObject, arrayName: String, processMD: ProcessMetadata) {
//        val currentProcessPayload = inputEvent[arrayName]
//        if (currentProcessPayload == null) {
//            inputEvent.add(arrayName,  JsonArray())
//        }
//        val currentArray = inputEvent[arrayName].asJsonArray
//        currentArray.add(processMD.toJsonElement())
//    }
}