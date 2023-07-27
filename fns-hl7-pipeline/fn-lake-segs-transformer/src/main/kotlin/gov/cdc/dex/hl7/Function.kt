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
import gov.cdc.dex.hl7.model.Segment
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import java.util.*
import org.slf4j.LoggerFactory

/**
 * Azure function with event hub trigger for the Lake of Segments transformer
 * Takes an HL7 message and converts it to a lake of segments based on the HL7 dependency tree
 */
class Function {
    
    companion object {

        const val PROCESS_STATUS_OK = "SUCCESS"
        const val PROCESS_STATUS_EXCEPTION = "FAILURE"
        const val SUMMARY_STATUS_OK = "LAKE-SEGMENTS-TRANSFORMED"
        const val SUMMARY_STATUS_ERROR = "LAKE-SEGMENTS-ERROR"
        val fnConfig = FunctionConfig()
        private val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create()
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
        @BindingName("SystemPropertiesArray") eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext) : JsonObject {

        return processMessages(message, eventHubMD)

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
        context: ExecutionContext) : JsonObject {

        return processMessages(message, eventHubMD)

    } // .eventHubProcessor

    private fun processMessages(message: List<String?>, eventHubMD: List<EventHubMetadata>) : JsonObject {
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
                val profileFilePath = "/BasicProfile.json"
                val config = listOf(profileFilePath)
                try {
                    // read the profile
                    val profile = this::class.java.getResource(profileFilePath).readText()

                    // Transform to Lake of Segments
                    val lakeSegsModel = TransformerSegments().hl7ToSegments(hl7Content, profile)
                    logger.info("DEX::Processed OK for Lake of Segments messageUUID: $messageUUID, filePath: $filePath, ehDestination: ${fnConfig.eventHubSendOkName}")

                    // deliver
                    updateMetadataAndDeliver(startTime, PROCESS_STATUS_OK, lakeSegsModel, eventHubMD[messageIndex],
                        fnConfig.evHubSender, fnConfig.eventHubSendOkName, inputEvent, null, config
                    )

                    return inputEvent
                } catch (e: Exception) {

                    logger.error("DEX::Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")

                    //publishing the message  to the eventhubSendErrsName topic using EventHub
                    updateMetadataAndDeliver(startTime, PROCESS_STATUS_EXCEPTION, null, eventHubMD[messageIndex],
                        fnConfig.evHubSender, fnConfig.eventHubSendErrsName, inputEvent, e, config)

                    logger.info("Processed ERROR for Lake of Segments Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: ${fnConfig.eventHubSendErrsName}")

                    return inputEvent
                } // .catch

            } catch (e: Exception) {

                // message is bad, can't extract fields based on schema expected
                logger.error("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()
                return JsonObject()
            } // .catch

        } // .message.forEach
        return JsonObject()
    }
    private fun updateMetadataAndDeliver(startTime: String, status: String, report: List<Segment>?, eventHubMD: EventHubMetadata,
                                         evHubSender: EventHubSender, evTopicName: String, inputEvent: JsonObject, exception: Exception?, config: List<String>) {

        val processMD = LakeSegsTransProcessMetadata(status=status, report=report, eventHubMD = eventHubMD, config)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()

        val metadata =  JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject
        metadata.addArrayElement("processes", processMD)

        if (exception != null) {
            //TODO::  - update retry counts
            val problem = Problem(LakeSegsTransProcessMetadata.PROCESS_NAME, exception, false, 0, 0)
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
