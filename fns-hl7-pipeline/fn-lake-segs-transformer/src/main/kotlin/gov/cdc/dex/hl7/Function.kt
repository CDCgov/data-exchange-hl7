package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.model.Segment
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import org.slf4j.LoggerFactory
import java.util.*
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
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%",)
        messages: List<String?>,
        @BindingName("SystemPropertiesArray") eventHubMD:List<EventHubMetadata>,
    ): List<String> {
        val outList = mutableListOf<String>()
        val profileFilePath = "/BasicProfile.json"
        val config = listOf(profileFilePath)

        messages.forEachIndexed { messageIndex: Int, singleMessage: String? ->
            if (!singleMessage.isNullOrEmpty()) {
                val startTime = Date().toIsoString()
                val inputEvent = JsonParser.parseString(singleMessage) as JsonObject
                val hl7Content: String
                val metadata: JsonObject
                val filePath: String
                val messageUUID: String
                try {
                    val hl7ContentBase64 = inputEvent["content"].asString
                    val hl7ContentDecodedBytes = getDecoder().decode(hl7ContentBase64)
                    hl7Content = String(hl7ContentDecodedBytes)
                    metadata = inputEvent["metadata"].asJsonObject
                    val provenance = metadata["provenance"].asJsonObject
                    filePath = provenance["file_path"].asString
                    messageUUID = inputEvent["message_uuid"].asString

                    var status: String
                    var lakeSegsModel: List<Segment>?
                    var exception: Exception?

                    logger.info("DEX::Received and Processing messageUUID: $messageUUID, filePath: $filePath")
                    try {
                        // read the profile
                        val profile = this::class.java.getResource(profileFilePath).readText()
                        // Transform to Lake of Segments
                        lakeSegsModel = TransformerSegments().hl7ToSegments(hl7Content, profile)
                        status = PROCESS_STATUS_OK
                        exception = null
                        logger.info("DEX::Processed OK for Lake of Segments messageUUID: $messageUUID, filePath: $filePath, ehDestination: ${fnConfig.evHubSendName}")
                    } catch (e: Exception) {
                        status = PROCESS_STATUS_EXCEPTION
                        logger.error("DEX::Exception: Unable to process Message messageUUID: $messageUUID, filePath: $filePath, due to exception: ${e.message}")
                        lakeSegsModel = null
                        exception = e
                    } // .catch
                    // update Metadata and add to batch for delivery
                     updateMetadataAndDeliver(
                        startTime,
                        status,
                        lakeSegsModel,
                        eventHubMD[messageIndex],
                        inputEvent,
                        exception,
                        config,
                        outList
                    )

                } catch (e: Exception) {
                    //TODO::  - update retry counts
                    logger.error("DEX:: Unable to process Message due to exception: ${e.message}")
                    updateMetadataAndDeliver(startTime = startTime, status = PROCESS_STATUS_EXCEPTION,
                        report =null, eventHubMD = eventHubMD[messageIndex], inputEvent = inputEvent,
                        exception = e, config = config, outList = outList)
                } // .try
            } // .if
        }// .foreach

        // send everything to out event hub
        try {
            fnConfig.evHubSender.send(outList)
            logger.info("Sent batch of ${outList.size} messages to ${fnConfig.evHubSendName}")
        } catch (e : Exception) {
            logger.error("Unable to send to event hub ${fnConfig.evHubSendName}: ${e.message}")
        }

        return outList

    } // .eventHubProcessor

    private fun updateMetadataAndDeliver(startTime: String,
                                         status: String,
                                         report: List<Segment>?,
                                         eventHubMD: EventHubMetadata,
                                         inputEvent: JsonObject,
                                         exception: Exception?,
                                         config: List<String>,
                                         outList: MutableList<String>) {

        val processMD = LakeSegsTransProcessMetadata(status=status, output=report, eventHubMD = eventHubMD, config)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()

        val metadata =  JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject
        metadata.remove("processes")
        metadata.add("stage", processMD.toJsonElement())

        if (exception != null) {
            //TODO::  - update retry counts
            val problem = Problem(LakeSegsTransProcessMetadata.PROCESS_NAME, exception, false, 0, 0)
            val summary = SummaryInfo(SUMMARY_STATUS_ERROR, problem)
            inputEvent.add("summary", summary.toJsonElement())
        } else {
            inputEvent.add("summary", (SummaryInfo(SUMMARY_STATUS_OK, null).toJsonElement()))
        }
        inputEvent.remove("content")
        // add to out event hub list
        outList.add(gsonWithNullsOn.toJson(inputEvent))
    }


} // .Function
