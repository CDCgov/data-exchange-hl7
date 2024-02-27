package gov.cdc.dataexchange

import com.azure.core.util.BinaryData
import com.google.gson.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.util.JsonHelper
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log

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
                logger.error("ERROR: Unable to parse event message with index $index as JSON. Aborting save.")
                null
            } ?: continue
            //remove content
            inputEvent.remove("content")

            val messageUUID = try {
                // this is where the value will be in hl7 message json
                JsonHelper.getValueFromJson("message_metadata.message_uuid", inputEvent).asString
            } catch (e: Exception) {
                "ERROR: Unable to locate message_uuid or file_uuid for event message with index $index." +
                        " Aborting save."
                null
            } ?: continue

            // get the metadata needed for routing
            val routingMeta = inputEvent["routing_metadata"]
            if (!(routingMeta == null || routingMeta.isJsonNull)) {
                val routingMetadata = routingMeta.asJsonObject
                val supportingMetadata = routingMetadata.remove("supporting_metadata")
                val metaToAttach = mutableMapOf<String, String>()
                routingMetadata.asMap().forEach { entry ->
                    if (!entry.value.isJsonNull) {
                        metaToAttach[entry.key] = entry.value.asString
                    }
                }
                if (!supportingMetadata.isJsonNull) {
                    supportingMetadata.asJsonObject.asMap().forEach { entry ->
                        if (!entry.value.isJsonNull) {
                            metaToAttach.putIfAbsent(entry.key, entry.value.asString)
                        }
                    }
                }
            } else {
                logger.error("DEX::ERROR:Unable to locate routing_metadata")
            }
            logger.info("DEX::Processing message $messageUUID")
            try {
                val folderStructure = SimpleDateFormat("YYYY/MM/dd").format(Date())
                // save to storage container
                this.saveBlobToContainer(
                    "${fnConfig.blobStorageFolderName}/$folderStructure/$messageUUID.txt",
                    gson.toJson(inputEvent),
                    metaToAttach
                )
                logger.info("DEX::Saved message $messageUUID.txt to sink ${fnConfig.blobStorageContainerName}/${fnConfig.blobStorageFolderName}")
            } catch (e: Exception) {
                // TODO send to quarantine?
                logger.error("DEX::Error processing message", e)
                throw Exception("Failure in file sink :${e.message}")
            }
        }

    }

    fun saveBlobToContainer(blobName: String, blobContent: String, newMetadata: MutableMap<String, String>) {
        val data = BinaryData.fromString(blobContent)
        val client = fnConfig.azureBlobProxy.getBlobClient(blobName)
        client.upload(data, true)
        client.setMetadata(newMetadata)

    }
}