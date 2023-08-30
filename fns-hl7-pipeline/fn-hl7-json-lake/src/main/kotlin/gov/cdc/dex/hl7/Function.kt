package gov.cdc.dex.hl7

import com.google.gson.*
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.bumblebee.HL7JsonTransformer
import java.util.*
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory


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

        val fnConfig = FunctionConfig()
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    } // .companion object


    @FunctionName("HL7_JSON_LAKE_TRANSFORMER")
    fun eventHubProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%",)
        messages: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext): JsonObject {

        //
        // Process each Event Hub Message
        // ----------------------------------------------
        // message.forEach { singleMessage: String? ->
        logger.info("DEX::${context.functionName}")

        return processAllMessages(messages, eventHubMD, context) // .message.forEach

    } // .eventHubProcessor

    private fun processAllMessages( messages: List<String?>, eventHubMD: List<EventHubMetadata>, context: ExecutionContext):JsonObject {
        messages.forEachIndexed { messageIndex: Int, singleMessage: String? ->
            val startTime = Date().toIsoString()
            try {
                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                val metadata = JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject
                val filePath =JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                val messageUUID = inputEvent["message_uuid"].asString

                logger.info("DEX::Processing messageUUID:$messageUUID")
                try {
                    val hl7message = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
                    val bumblebee = HL7JsonTransformer.getTransformerWithResource(hl7message, FunctionConfig.PROFILE_FILE_PATH)
                    val fullHL7 = bumblebee.transformMessage()
                    updateMetadataAndDeliver(startTime, metadata, PROCESS_STATUS_OK, fullHL7, eventHubMD[messageIndex],
                        fnConfig.evHubSender, fnConfig.eventHubSendOkName, gsonWithNullsOn, inputEvent, null,
                        listOf(FunctionConfig.PROFILE_FILE_PATH))
                    context.logger.info("Processed OK for HL7 JSON Lake messageUUID: $messageUUID, filePath: $filePath, ehDestination: ${fnConfig.eventHubSendOkName}")
                    
                    if (messageIndex == messages.lastIndex){
                        return inputEvent
                    }

                } catch (e: Exception) {
                    context.logger.severe("Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")
                    //publishing the message  to the eventhubSendErrsName topic using EventHub
                    updateMetadataAndDeliver(startTime, metadata, PROCESS_STATUS_EXCEPTION, null, eventHubMD[messageIndex],
                        fnConfig.evHubSender, fnConfig.eventHubSendErrsName, gsonWithNullsOn, inputEvent, e,
                        listOf(FunctionConfig.PROFILE_FILE_PATH))

                    context.logger.info("Processed ERROR for HL7 JSON Lake messageUUID: $messageUUID, filePath: $filePath, ehDestination: ${fnConfig.eventHubSendErrsName}")
                    return inputEvent
                } // .catch

            } catch (e: Exception) {
                // message is bad, can't extract fields based on schema expected
                context.logger.severe("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()
                return JsonObject()

            } // .catch

        }
        return JsonObject()
    }

    private fun updateMetadataAndDeliver(startTime: String, metadata: JsonObject, status: String, report: JsonObject?, eventHubMD: EventHubMetadata,
                                         evHubSender: EventHubSender, evTopicName: String, gsonWithNullsOn: Gson, inputEvent: JsonObject, exception: Exception?, config: List<String>) {

        val processMD = HL7JSONLakeProcessMetadata(status=status, report=report, eventHubMD = eventHubMD, config)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()
        metadata.addArrayElement("processes", processMD)

        if (exception != null) {
            //TODO::  - update retry counts
            val problem = Problem(HL7JSONLakeProcessMetadata.PROCESS_NAME, exception, false, 0, 0)
            val summary = SummaryInfo(SUMMARY_STATUS_ERROR, problem)
            inputEvent.add("summary", summary.toJsonElement())
        } else {
            inputEvent.add("summary", (SummaryInfo(SUMMARY_STATUS_OK, null).toJsonElement()))
        }
        // enable for model
        val inputEventOut = gsonWithNullsOn.toJson(inputEvent)
        evHubSender.send(
            evHubTopicName = evTopicName,
            message = inputEventOut
        )

    }

} // .Function

