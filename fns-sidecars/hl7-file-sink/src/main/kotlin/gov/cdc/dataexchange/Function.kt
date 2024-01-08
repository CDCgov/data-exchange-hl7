package gov.cdc.dataexchange

import com.azure.core.util.BinaryData
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

        messages.forEach { message: String? ->
            try {
                // extract metadata
                val inputEvent: JsonObject = JsonParser.parseString(message) as JsonObject
                val messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString
                val originalDestId = try {
                    JsonHelper.getValueFromJson("destination_id", inputEvent).asString
                } catch (e : Exception) {
                    "unknown"
                }
                logger.info("DEX::Processing message $messageUUID")

                // add metadata
                val newMetadata = mutableMapOf<String, String>()
                newMetadata["meta_destination_id"] = originalDestId

                // change event to match destination folder name
                inputEvent.addProperty("meta_ext_event", fnConfig.blobStorageFolderName)
                newMetadata["meta_ext_event"] = fnConfig.blobStorageFolderName
                val folderStructure = LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY/MM/dd"))

                // save to storage container
                this.saveBlobToContainer("${fnConfig.blobStorageFolderName}/$folderStructure/$messageUUID.txt", gson.toJson(inputEvent), newMetadata)
                logger.info("DEX::Saved message $messageUUID.txt to sink ${fnConfig.blobStorageContainerName}/${fnConfig.blobStorageFolderName}")
            } catch (e: Exception) {
                // TODO send to quarantine?
                logger.error("DEX::Error processing message", e)
                throw Exception("Failure in file sink ::${e.message}")
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