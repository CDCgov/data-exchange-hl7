package gov.cdc.dataexchange

import com.azure.core.util.BinaryData
import com.google.gson.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.util.JsonHelper
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*

class Function {

    companion object {
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        val fnConfig = FunctionConfig()
        val gson = GsonBuilder().serializeNulls().create()!!
    }

    @FunctionName("storage-sink")
    fun storageSink(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%"
        ) messages: List<String?>
    ) {
        logger.info("DEX::Received event!")

        for ((index, message) in messages.withIndex()) {

            // extract metadata
            val inputEvent = try {
                JsonParser.parseString(message) as JsonObject
            } catch (e: Exception) {
                logger.error("ERROR: Unable to parse event message #$index as JSON. Aborting save.")
                null
            } ?: continue

            val messageUUID = try {
                // this is where the value will be in hl7 message json
                JsonHelper.getValueFromJson("message_uuid", inputEvent).asString
            } catch (e: Exception) {
                try {
                    // this is where the value will be in recdeb-reports json
                    JsonHelper.getValueFromJson("file_uuid", inputEvent).asString
                } catch (e: Exception) {
                    logger.error(
                        "ERROR: Unable to locate message_uuid or file_uuid for event message #$index." +
                                " Aborting save."
                    )
                    null
                }
            } ?: continue

            val originalDestId = getValueOrUnknown("routing_metadata.destination_id", inputEvent)
            val uploadID = getValueOrUnknown("routing_metadata.upload_id", inputEvent)
            val traceID = getValueOrUnknown("routing_metadata.trace_id", inputEvent)
            val parentSpanID = getValueOrUnknown("routing_metadata.parent_span_id", inputEvent)

            logger.info("DEX::Processing message $messageUUID")
            try {
                // add metadata
                val newMetadata = mutableMapOf<String, String>()
                newMetadata["meta_destination_id"] = originalDestId
                newMetadata["meta_ext_uploadid"] = uploadID
                newMetadata["trace_id"] = traceID
                newMetadata["parent_span_id"] = parentSpanID
                // change event to match destination folder name
                inputEvent.addProperty("meta_ext_event", fnConfig.blobStorageFolderName)
                newMetadata["meta_ext_event"] = fnConfig.blobStorageFolderName
                val folderStructure = SimpleDateFormat("YYYY/MM/DD").format(Date())

                // save to storage container
                this.saveBlobToContainer(
                    "${fnConfig.blobStorageFolderName}/$folderStructure/$messageUUID.txt",
                    gson.toJson(inputEvent),
                    newMetadata
                )
                logger.info("DEX::Saved message $messageUUID.txt to sink ${fnConfig.blobStorageContainerName}/${fnConfig.blobStorageFolderName}")
            } catch (e: Exception) {
                // TODO send to quarantine?
                logger.error("DEX::Error processing message", e)
                throw Exception("Failure in file sink :${e.message}")
            }
        }

    }

    private fun getValueOrUnknown(jsonPath: String, inputEvent: JsonObject): String {
        return try {
            JsonHelper.getValueFromJson(jsonPath, inputEvent).asString
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    fun saveBlobToContainer(blobName: String, blobContent: String, newMetadata: MutableMap<String, String>) {
        val data = BinaryData.fromString(blobContent)
        val client = fnConfig.azureBlobProxy.getBlobClient(blobName)
        client.upload(data, true)
        client.setMetadata(newMetadata)

    }
}