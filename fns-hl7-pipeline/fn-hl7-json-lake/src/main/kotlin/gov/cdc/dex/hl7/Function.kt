package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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



/**
 * Azure function with event hub trigger for the HL7 JSON Lake transformer
 * Takes an HL7 message and converts it to an HL7 json lake based on the PhinGuidProfile
 */
class Function {

    companion object {

        val PROCESS_STATUS_OK = "SUCCESS"
        val PROCESS_STATUS_EXCEPTION = "FAILURE"

        val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create()

        val fnConfig = FunctionConfig()
    } // .companion object


    @FunctionName("HL7_JSON_LAKE_TRANSFORMER_CASE")
    fun eventHubCASEProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveNameCASE%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroupCASE%",)
        messages: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext) {

        //
        // Process each Event Hub Message
        // ----------------------------------------------
        // message.forEach { singleMessage: String? ->
        processAllMessages(messages, eventHubMD, context) // .message.forEach

    } // .eventHubProcessor
    @FunctionName("HL7_JSON_LAKE_TRANSFORMER_ELR")
    fun eventHubELRProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveNameELR%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroupELR%",)
        messages: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext) {
        //
        // Process each Event Hub Message
        // ----------------------------------------------
        // message.forEach { singleMessage: String? ->
        processAllMessages(messages, eventHubMD, context)

    } // .eventHubProcessor

    private fun processAllMessages( messages: List<String?>, eventHubMD: List<EventHubMetadata>, context: ExecutionContext) {
        messages.forEachIndexed { messageIndex: Int, singleMessage: String? ->
            val startTime = Date().toIsoString()
            try {
                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                val metadata = JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject
                val filePath =JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                val messageUUID = inputEvent["message_uuid"].asString

                try {
                    processMessage(
                        inputEvent,
                        eventHubMD[messageIndex],
                        startTime,
                        metadata,
                        fnConfig.eventHubSendOkName
                    )
                } catch (e: Exception) {
                    context.logger.severe("DEX::Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")
                    //publishing the message  to the eventhubSendErrsName topic using EventHub
                    processMessageError(e,inputEvent,fnConfig.eventHubSendOkName,fnConfig.evHubSender)

                    context.logger.info("DEX::Processed for HL7 JSON Lake messageUUID: $messageUUID, filePath: $filePath, ehDestination: ${fnConfig.eventHubSendErrsName}")
                } // .catch

            } catch (e: Exception) {
                // message is bad, can't extract fields based on schema expected
                context.logger.severe("DEX::Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()

            } // .catch

        }
    }

    private fun processMessage(
        inputEvent: JsonObject,
        eventHubMD: EventHubMetadata,
        startTime: String,
        metadata: JsonObject,
        eventHubSendOkName: String
    ) {
        val hl7message = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
        val bumblebee = HL7JsonTransformer.getTransformerWithResource(hl7message, FunctionConfig.PROFILE_FILE_PATH)
        val fullHL7 = bumblebee.transformMessage()

        val processMD = HL7JSONLakeProcessMetadata(
            status = PROCESS_STATUS_OK, eventHubMD = eventHubMD, report = fullHL7,
            config = listOf(FunctionConfig.PROFILE_FILE_PATH)
        )

        // process time
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()

        metadata.addArrayElement("processes", processMD)

        // enable for model
        fnConfig.evHubSender.send(evHubTopicName = eventHubSendOkName, message = gsonWithNullsOn.toJson(inputEvent))
    }





    private fun processMessageError(
        e: Exception, inputEvent: JsonObject, eventHubSendErrsName: String, evHubSender: EventHubSender) {
        //TODO::  - update retry counts
        val problem = Problem(HL7JSONLakeProcessMetadata.PROCESS_NAME, e, false, 0, 0)
        val summary = SummaryInfo(PROCESS_STATUS_EXCEPTION, problem)
        inputEvent.add("summary", summary.toJsonElement())

        evHubSender.send(evHubTopicName = eventHubSendErrsName, message = gsonWithNullsOn.toJson(inputEvent))
    }

} // .Function

