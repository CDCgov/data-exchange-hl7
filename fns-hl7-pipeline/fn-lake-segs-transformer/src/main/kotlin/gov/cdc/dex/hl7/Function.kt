package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.OutputBinding
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.EventHubMetadata

import gov.cdc.dex.hl7.model.Segment
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.gson
import gov.cdc.dex.util.JsonHelper.toJsonElement
import java.util.*
import org.slf4j.LoggerFactory
import java.util.Base64.*

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
    fun eventHubProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiverName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%",)
        message: List<String?>,
        @BindingName("SystemPropertiesArray") eventHubMD:List<EventHubMetadata>,
        @EventHubOutput(name="lakeSegsOk",
            eventHubName = "%EventHubSendOkName%",
            connection = "EventHubConnectionString") lakeSegsOk : OutputBinding<List<String>>,
        @EventHubOutput(name="lakeSegsErr",
            eventHubName = "%EventHubSendErrsName%",
            connection = "EventHubConnectionString") lakeSegsErr: OutputBinding<List<String>>,
        @CosmosDBOutput(name="cosmosdevpublic",
            connection = "CosmosDBConnectionString",
            containerName = "hl7-lakeSegs", createIfNotExists = true,
         partitionKey = "/message_uuid", databaseName = "hl7-events") cosmosOutput: OutputBinding<List<JsonObject>>,
        context: ExecutionContext
    ): List<JsonObject> {

        val processedMsgs = mutableListOf<JsonObject>()
        val outOkList = mutableListOf<String>()
        val outErrList = mutableListOf<String>()
        val outEventList = mutableListOf<JsonObject>()
        //process the messages
        message.forEachIndexed {
                messageIndex: Int, singleMessage: String? ->
            //context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            val startTime =  Date().toIsoString()
            try {

                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                // context.logger.info("------ inputEvent: ------>: --> $inputEvent")
                // Extract from event
                val hl7ContentBase64 = inputEvent["content"].asString
                val hl7ContentDecodedBytes = getDecoder().decode(hl7ContentBase64)
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
                    outOkList.add(gson.toJson(inputEvent))
                    outEventList.add(gson.toJsonTree(inputEvent) as JsonObject)
                    logger.info("DEX::Processed OK for Lake of Segments messageUUID: $messageUUID, filePath: $filePath, ehDestination: ${fnConfig.eventHubSendOkName}")

                    // deliver
                    outOkList.add(gson.toJson(inputEvent))
                    outEventList.add(gson.toJsonTree(inputEvent) as JsonObject)
                    updateMetadataAndDeliver(startTime, PROCESS_STATUS_OK, lakeSegsModel, eventHubMD[messageIndex],inputEvent, null, config
                    )

                    processedMsgs.add( inputEvent )
                } catch (e: Exception) {

                    logger.error("DEX::Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")

                    //publishing the message  to the eventhubSendErrsName topic using EventHub
                    outErrList.add(gson.toJson(inputEvent))
                    outEventList.add(gson.toJsonTree(inputEvent) as JsonObject)
                    updateMetadataAndDeliver(startTime, PROCESS_STATUS_EXCEPTION, null, eventHubMD[messageIndex], inputEvent, e, config)

                    logger.info("Processed ERROR for Lake of Segments Model messageUUID: $messageUUID, filePath: $filePath, ehDestination: ${fnConfig.eventHubSendErrsName}")

                    processedMsgs.add( inputEvent )
                } // .catch

            } catch (e: Exception) {

                // message is bad, can't extract fields based on schema expected
                logger.error("Unable to process Message due to exception: ${e.message}")
                e.printStackTrace()
                processedMsgs.add( JsonObject() )
            } // .catch

        } // .message.forEach
        //add payload to eventhubs and cosmosdb outbindings
        lakeSegsOk.value = outOkList
        lakeSegsErr.value = outErrList
        cosmosOutput.value = outEventList.toList()
        return processedMsgs.toList()

    } // .eventHubProcessor

    private fun updateMetadataAndDeliver(startTime: String, status: String, report: List<Segment>?, eventHubMD: EventHubMetadata, inputEvent: JsonObject, exception: Exception?, config: List<String>) {

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
    }


} // .Function
