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

    } // .companion object


    @FunctionName("HL7_JSON_LAKE_TRANSFORMER_CASE")
    fun eventHubCASEProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveNameCASE%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerCASEGroup%",)
        message: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext) {

        // context.logger.info("------ received event: ------> message: --> $message")

        val startTime =  Date().toIsoString()

        val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()

        // Set up the 2 out Event Hubs: OK and Errs
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val eventHubSendOkName = System.getenv("EventHubSendOkName")
        val eventHubSendErrsName = System.getenv("EventHubSendErrsName")
        val evHubSender = EventHubSender(evHubConnStr)

        //
        // Process each Event Hub Message
        // ----------------------------------------------
        // message.forEach { singleMessage: String? ->
        message.forEachIndexed {
                messageIndex: Int, singleMessage: String? ->
            // context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            try {

                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                // context.logger.info("------ inputEvent: ------>: --> $inputEvent")
                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject
                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString

                // read the profile
                val profileFilePath = "/PhinGuidProfile.json"
                val profile = this::class.java.getResource(profileFilePath).readText()
                try {
                    val bumblebee = HL7JsonTransformer.getTransformerWithResource(inputEvent.asString, profile)
                    val fullHL7 = bumblebee.transformMessage()

                    val processMD = HL7JSONLakeProcessMetadata(
                        status = PROCESS_STATUS_OK, eventHubMD = eventHubMD[messageIndex], report = fullHL7,
                        config = listOf(profileFilePath)
                    )

                    // process time
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()

                    metadata.addArrayElement("processes", processMD)

                    // enable for model

                    evHubSender.send(evHubTopicName = eventHubSendOkName, message = gsonWithNullsOn.toJson(inputEvent))
                } catch (e: Exception) {

                    context.logger.severe("Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")
                    //publishing the message  to the eventhubSendErrsName topic using EventHub
                    processMessageError(e, inputEvent, eventHubSendOkName, evHubSender, gsonWithNullsOn)

                    context.logger.info("Processed for HL7 JSON Lake messageUUID: $messageUUID, filePath: $filePath, ehDestination: $eventHubSendErrsName")
                } // .catch

            } catch (e: Exception) {

                // message is bad, can't extract fields based on schema expected
                context.logger.severe("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()

            } // .catch

        } // .message.forEach

    } // .eventHubProcessor



    @FunctionName("HL7_JSON_LAKE_TRANSFORMER_ELR")
    fun eventHubELRProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveNameELR%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerELRGroup%",)
        message: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext) {

        // context.logger.info("------ received event: ------> message: --> $message")

        val startTime =  Date().toIsoString()

        val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()

        // Set up the 2 out Event Hubs: OK and Errs
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val eventHubSendELROkName = System.getenv("EventHubSendELROkName")
        val eventHubSendErrsName = System.getenv("EventHubSendErrsName")
        val evHubSender = EventHubSender(evHubConnStr)

        //
        // Process each Event Hub Message
        // ----------------------------------------------
        // message.forEach { singleMessage: String? ->
        message.forEachIndexed {
                messageIndex: Int, singleMessage: String? ->
            // context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            try {

                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                // context.logger.info("------ inputEvent: ------>: --> $inputEvent")
                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject
                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString

                try {
                    // read the profile
                    val profileFilePath = "/PhinGuidProfile.json"
                    val profile = this::class.java.getResource(profileFilePath).readText()

                    val bumblebee = HL7JsonTransformer.getTransformerWithResource(inputEvent.asString, profile)
                    val fullHL7 = bumblebee.transformMessage()

                    val processMD = HL7JSONLakeProcessMetadata(
                        status = PROCESS_STATUS_OK, eventHubMD = eventHubMD[messageIndex], report = fullHL7,
                        config = listOf(profileFilePath)
                    )

                    // process time
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()

                    metadata.addArrayElement("processes", processMD)

                    // enable for model

                    evHubSender.send(
                        evHubTopicName = eventHubSendELROkName,
                        message = gsonWithNullsOn.toJson(inputEvent)
                    )
                }
                catch (e: Exception) {

                    context.logger.severe("Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")
                    //publishing the message  to the eventhubSendErrsName topic using EventHub
                    processMessageError(e, inputEvent, eventHubSendErrsName, evHubSender, gsonWithNullsOn)

                    context.logger.info("Processed for HL7 JSON Lake messageUUID: $messageUUID, filePath: $filePath, ehDestination: $eventHubSendErrsName")
                } // .catch


            } catch (e: Exception) {

                // message is bad, can't extract fields based on schema expected
                context.logger.severe("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()

            } // .catch

        } // .message.forEach

    } // .eventHubProcessor


    private fun processMessageError(
        e: Exception,
        inputEvent: JsonObject,
        eventHubSendErrsName: String,
        evHubSender: EventHubSender,
        gsonWithNullsOn: Gson
    ) {
        //TODO::  - update retry counts
        val problem = Problem(HL7JSONLakeProcessMetadata.PROCESS_NAME, e, false, 0, 0)
        val summary = SummaryInfo(PROCESS_STATUS_EXCEPTION, problem)
        inputEvent.add("summary", summary.toJsonElement())

        evHubSender.send(evHubTopicName = eventHubSendErrsName, message = gsonWithNullsOn.toJson(inputEvent))
    }

} // .Function

