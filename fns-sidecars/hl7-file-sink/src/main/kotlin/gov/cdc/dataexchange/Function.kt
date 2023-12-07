package gov.cdc.dataexchange

import com.azure.core.util.BinaryData
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import org.slf4j.LoggerFactory

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
                logger.info("DEX::Processing message $messageUUID")

                // add metadata
                inputEvent.add("meta_destination_id", "dex-hl7".toJsonElement())

                // change event to match destination folder name
                inputEvent.addProperty("meta_ext_event", fnConfig.blobStorageFolderName)

                // save to storage container
                this.saveBlobToContainer("${fnConfig.blobStorageFolderName}/$messageUUID.txt", gson.toJson(inputEvent))
                logger.info("DEX::Saved message $messageUUID.txt to sink ${fnConfig.blobStorageContainerName}/${fnConfig.blobStorageFolderName}")
            } catch (e: Exception) {
                // TODO send to quarantine?
                logger.error("DEX::Error processing message", e)
            }
        }

    }

    fun saveBlobToContainer(blobName: String, blobContent: String) {
        val data = BinaryData.fromString(blobContent)
        val client = fnConfig.azureBlobProxy.getBlobClient(blobName)
        client.upload(data, true)
    }
}