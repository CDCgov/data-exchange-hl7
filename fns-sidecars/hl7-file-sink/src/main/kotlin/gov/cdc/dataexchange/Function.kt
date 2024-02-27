package gov.cdc.dataexchange

import com.azure.core.util.BinaryData
import com.google.gson.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.UnknownPropertyError
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
                logger.error("ERROR: Unable to parse event message with index $index as JSON. Aborting save.")
                null
            } ?: continue

            val messageUUID = try {
                // this is where the value will be in hl7 message json
                JsonHelper.getValueFromJson("message_metadata.message_uuid", inputEvent).asString
            } catch (e: Exception) {
                null
            }
            val uploadId = try {
                JsonHelper.getValueFromJson("routing_metadata.upload_id", inputEvent).asString
            } catch (e: Exception) {
                null
            }
            if (messageUUID == null && uploadId == null) {
                logger.error("DEX::ERROR -- No Message UUID or Upload ID found. Aborting save for message index $index")
                continue
            }
            val newBlobName = messageUUID ?: uploadId
            // get the metadata needed for routing
            val metaToAttach = mutableMapOf<String, String>()
            val routingMeta = try {
                JsonHelper.getValueFromJson("routing_metadata", inputEvent)
            } catch (e: UnknownPropertyError) {
                JsonNull.INSTANCE
            }
            if (!routingMeta.isJsonNull) {
                val routingMetadata = routingMeta.asJsonObject
                val supportingMeta = routingMetadata.remove("supporting_metadata")
                routingMetadata.keySet().forEach { key ->
                    if (!routingMetadata[key].isJsonNull) {
                        metaToAttach[key] = routingMetadata[key].asString
                    }
                }
                if (!supportingMeta.isJsonNull ) {
                    val supportingMetadata = supportingMeta.asJsonObject
                    supportingMetadata.keySet().forEach { key ->
                        if (!supportingMetadata[key].isJsonNull)
                            metaToAttach.putIfAbsent(key, supportingMetadata[key].asString)

                    }
                }
            } else {
                logger.error("DEX::ERROR:Unable to locate routing_metadata for message $newBlobName")
            }
            logger.info("DEX::Saving message $newBlobName")
            try {
                val folderStructure = SimpleDateFormat("YYYY/MM/dd").format(Date())
                // save to storage container
                this.saveBlobToContainer(
                    "${fnConfig.blobStorageFolderName}/$folderStructure/$newBlobName.txt",
                    gson.toJson(inputEvent),
                    metaToAttach
                )
                logger.info("DEX::Saved message $newBlobName.txt to sink ${fnConfig.blobStorageContainerName}/${fnConfig.blobStorageFolderName}")
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