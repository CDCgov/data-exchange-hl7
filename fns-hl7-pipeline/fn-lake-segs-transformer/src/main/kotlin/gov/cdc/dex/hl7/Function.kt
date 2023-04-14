package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import com.google.gson.Gson
import com.google.gson.GsonBuilder

import com.google.gson.JsonObject
import com.google.gson.JsonParser

import java.util.*

import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement

import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo


/**
 * Azure function with event hub trigger for the Lake of Segments transformer
 * Takes an HL7 message and converts it to an lake of segments based on the HL7 dependency tree
 */
class Function {
    
    companion object {

        // same in LakeSegsTransProcessMetadata
        const val PROCESS_NAME = "lakeSegsTransformer"
        // const val PROCESS_VERSION = "1.0.0"

        val PROCESS_STATUS_OK = "PROCESS_LAKE_SEGS_TRANSFORMER_OK"
        val PROCESS_STATUS_EXCEPTION = "PROCESS_LAKE_SEGS_TRANSFORMER_EXCEPTION"

    } // .companion object


    @FunctionName("lakeSegsCASETransformer")
    fun eventHubCASEProcessor(
            @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveNameCASE%",
                connection = "EventHubConnectionString",
                consumerGroup = "%EventHubConsumerGroupCASE%",)
                message: List<String?>,
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
        message.forEach { singleMessage: String? ->
            // context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            try {

                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                // context.logger.info("------ inputEvent: ------>: --> $inputEvent")

                // Extract from event
                val hl7ContentBase64 = inputEvent["content"].asString
                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = String(hl7ContentDecodedBytes)
                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject
                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString

                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                // 
                // Process Message for SQL Model
                // ----------------------------------------------
                try {
                    processMessage(
                        hl7Content,
                        startTime,
                        metadata,
                        eventHubSendOkName,
                        evHubSender,
                        gsonWithNullsOn,
                        inputEvent
                    )
                    context.logger.info("Processed for Lake of Segments messageUUID: $messageUUID, filePath: $filePath, ehDestination: $eventHubSendOkName")

                } catch (e: Exception) {

                    context.logger.severe("Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")

                    //publishing the message  to the eventhubSendErrsName topic using EventHub
                    processMessageError(e, inputEvent, eventHubSendErrsName, evHubSender, gsonWithNullsOn)

                    context.logger.info("Processed for Lake of Segments Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $eventHubSendErrsName")
                } // .catch

            } catch (e: Exception) {

               // message is bad, can't extract fields based on schema expected
                context.logger.severe("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()

            } // .catch

        } // .message.forEach
     
    } // .eventHubProcessor



    @FunctionName("lakeSegsELRTransformer")
    fun eventHubELRProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveNameELR%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroupELR%",)
        message: List<String?>,
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
        message.forEach { singleMessage: String? ->
            // context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            try {

                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                // context.logger.info("------ inputEvent: ------>: --> $inputEvent")

                // Extract from event
                val hl7ContentBase64 = inputEvent["content"].asString
                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = String(hl7ContentDecodedBytes)
                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject
                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString

                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                //
                // Process Message for SQL Model
                // ----------------------------------------------
                try {

                   processMessage(
                        hl7Content,
                        startTime,
                        metadata,
                        eventHubSendOkName,
                        evHubSender,
                        gsonWithNullsOn,
                        inputEvent
                    )

                    context.logger.info("Processed for Lake of Segments messageUUID: $messageUUID, filePath: $filePath, ehDestination: $eventHubSendOkName")

                } catch (e: Exception) {

                    context.logger.severe("Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")
                    //publishing the message  to the eventhubSendErrsName topic using EventHub
                    processMessageError(e, inputEvent, eventHubSendErrsName, evHubSender, gsonWithNullsOn)

                    context.logger.info("Processed for Lake of Segments Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $eventHubSendErrsName")
                } // .catch

            } catch (e: Exception) {

                // message is bad, can't extract fields based on schema expected
                context.logger.severe("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()

            } // .catch

        } // .message.forEach

    } // .eventHubProcessor

    private fun processMessage(
        hl7Content: String,
        startTime: String,
        metadata: JsonObject,
        eventHubSendELROkName: String,
        evHubSender: EventHubSender,
        gsonWithNullsOn: Gson,
        inputEvent: JsonObject
    ){
        // read the profile
        val profileFilePath = "/BasicProfile.json"
        val profile = this::class.java.getResource(profileFilePath).readText()

        // Transformer to Lake of Segments
        // ------------------------------------------------------------------------------
        val lakeSegsModel = TransformerSegments().hl7ToSegments(hl7Content, profile)

        val processMD = LakeSegsTransProcessMetadata(status = PROCESS_STATUS_OK, report = lakeSegsModel)

        // process time
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()

        metadata.addArrayElement("processes", processMD)

        // enable for model

        evHubSender.send(evHubTopicName = eventHubSendELROkName, message = gsonWithNullsOn.toJson(inputEvent))

    }

    private fun processMessageError(
        e: Exception,
        inputEvent: JsonObject,
        eventHubSendErrsName: String,
        evHubSender: EventHubSender,
        gsonWithNullsOn: Gson
    ) {
        //TODO::  - update retry counts
        val problem = Problem(PROCESS_NAME, e, false, 0, 0)
        val summary = SummaryInfo(PROCESS_STATUS_EXCEPTION, problem)
        inputEvent.add("summary", summary.toJsonElement())

        evHubSender.send(evHubTopicName = eventHubSendErrsName, message = gsonWithNullsOn.toJson(inputEvent))
    }

} // .Function

