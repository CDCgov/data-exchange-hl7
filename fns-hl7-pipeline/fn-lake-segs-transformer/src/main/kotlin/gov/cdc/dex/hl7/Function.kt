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
import java.util.*
import org.slf4j.LoggerFactory


/**
 * Azure function with event hub trigger for the Lake of Segments transformer
 * Takes an HL7 message and converts it to an lake of segments based on the HL7 dependency tree
 */
class Function {
    
    companion object {

        val PROCESS_STATUS_OK = "SUCCESS"
        val PROCESS_STATUS_EXCEPTION = "FAILURE"

        val fnConfig = FunctionConfig()
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)

    } // .companion object


    @FunctionName("LAKE_OF_SEGMENTS_TRANSFORMER_CASE")
    fun eventHubCASEProcessor(
        @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveNameCASE%",
                connection = "EventHubConnectionString",
                consumerGroup = "%EventHubConsumerGroupCASE%",)
                message: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext) {

        // context.logger.info("------ received event: ------> message: --> $message") 

        val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()

        // 
        // Process each Event Hub Message
        // ----------------------------------------------
       // message.forEach { singleMessage: String? ->
        message.forEachIndexed {
                messageIndex: Int, singleMessage: String? ->
            // context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            val startTime =  Date().toIsoString()
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

                logger.info("DEX::Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                // 
                // Process Message for SQL Model
                // ----------------------------------------------
                try {
                    processMessage(
                        hl7Content,
                        startTime,
                        metadata,
                        fnConfig.eventHubSendOkName,
                        fnConfig.evHubSender,
                        eventHubMD[messageIndex],
                        gsonWithNullsOn,
                        inputEvent
                    )
                    logger.info("DEX::Processed for Lake of Segments messageUUID: $messageUUID, filePath: $filePath, ehDestination: $fnConfig.eventHubSendOkName")

                } catch (e: Exception) {

                    logger.severe("DEX::Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")

                    //publishing the message  to the eventhubSendErrsName topic using EventHub
                    processMessageError(e, inputEvent, fnConfig.eventHubSendErrsName, fnConfig.evHubSender, gsonWithNullsOn)

                    logger.info("Processed for Lake of Segments Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $fnConfig.eventHubSendErrsName")
                } // .catch

            } catch (e: Exception) {

               // message is bad, can't extract fields based on schema expected
                logger.error("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()

            } // .catch

        } // .message.forEach
     
    } // .eventHubProcessor



    @FunctionName("LAKE_OF_SEGMENTS_TRANSFORMER_ELR")
    fun eventHubELRProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveNameELR%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroupELR%",)
        message: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext) {

        // context.logger.info("------ received event: ------> message: --> $message")

        val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()


        //
        // Process each Event Hub Message
        // ----------------------------------------------
       // message.forEach { singleMessage: String? ->
        message.forEachIndexed {
                messageIndex: Int, singleMessage: String? ->
            // context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            val startTime =  Date().toIsoString()
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

                logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                //
                // Process Message for SQL Model
                // ----------------------------------------------
                try {

                   processMessage(
                        hl7Content,
                        startTime,
                        metadata,
                        fnConfig.eventHubSendOkName,
                        fnConfig.evHubSender,
                        eventHubMD[messageIndex],
                        gsonWithNullsOn,
                        inputEvent
                    )

                    logger.info("DEX::Processed for Lake of Segments messageUUID: $messageUUID, filePath: $filePath, ehDestination: $fnConfig.eventHubSendOkName")

                } catch (e: Exception) {

                    logger.severe("DEX::Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")
                    //publishing the message  to the eventhubSendErrsName topic using EventHub
                    processMessageError(e, inputEvent, fnConfig.eventHubSendErrsName, fnConfig.evHubSender, gsonWithNullsOn)

                    logger.info("DEX::Processed for Lake of Segments Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: $fnConfig.eventHubSendErrsName")
                } // .catch

            } catch (e: Exception) {

                // message is bad, can't extract fields based on schema expected
                logger.severe("DEX::Unable to process Message due to exception: ${e.message}")

            } // .catch

        } // .message.forEach

    } // .eventHubProcessor

    private fun processMessage(
        hl7Content: String,
        startTime: String,
        metadata: JsonObject,
        eventHubSendELROkName: String,
        evHubSender: EventHubSender,
        eventHubMD: EventHubMetadata,
        gsonWithNullsOn: Gson,
        inputEvent: JsonObject
    ){
        // read the profile
        val profileFilePath = "/BasicProfile.json"
        val profile = this::class.java.getResource(profileFilePath).readText()

        // Transformer to Lake of Segments
        // ------------------------------------------------------------------------------
        val lakeSegsModel = TransformerSegments().hl7ToSegments(hl7Content, profile)

        val processMD = LakeSegsTransProcessMetadata(status = PROCESS_STATUS_OK, eventHubMD = eventHubMD, report = lakeSegsModel,
            config = listOf(profileFilePath)
        )

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
        val problem = Problem(LakeSegsTransProcessMetadata.PROCESS_NAME, e, false, 0, 0)
        val summary = SummaryInfo(PROCESS_STATUS_EXCEPTION, problem)
        inputEvent.add("summary", summary.toJsonElement())

        evHubSender.send(evHubTopicName = eventHubSendErrsName, message = gsonWithNullsOn.toJson(inputEvent))
    }

} // .Function

