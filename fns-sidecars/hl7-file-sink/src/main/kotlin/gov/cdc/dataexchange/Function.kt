package gov.cdc.dataexchange

import com.azure.core.util.BinaryData
import com.google.gson.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.UnknownPropertyError
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneOffset
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
            val (datePattern,blobStorageFolderName) = getFolderDate(fnConfig.blobStorageFolderName)

            //remove content except structure report
            if(blobStorageFolderName != "hl7_out_validation_report")   inputEvent.remove("content")
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

                // update data_stream_route to match destination folder name
                routingMetadata.addProperty("data_stream_route", blobStorageFolderName)

                // add all routing metadata json elements to blob metadata we will attach on upload
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
                // add data_stream_route to reflect the destination folder
                metaToAttach["data_stream_route"] = blobStorageFolderName
                logger.error("DEX::ERROR:Unable to locate routing_metadata for message $newBlobName")
            }

            logger.info("DEX::Saving message $newBlobName")
            try {
                //get date in folder structure
               val dateStructure = createDatefolders(datePattern)
                logger.info("DEX::dateStructure $dateStructure")
               // save to storage container
                this.saveBlobToContainer(
                    "$blobStorageFolderName/$dateStructure/$newBlobName.txt",
                    gson.toJson(inputEvent),
                    metaToAttach
                )
                logger.info("DEX::Saved message $newBlobName.txt to sink ${fnConfig.blobStorageContainerName}/$blobStorageFolderName/$dateStructure")
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
        client.setMetadata(newMetadata)
        client.upload(data, true)
    }

    fun getFolderDate( folder:String): Pair<String,String> {
        val splitPattern = folder.split("/")
        val datePattern = splitPattern.drop(1).joinToString("/")
        val folderName = splitPattern.first()
        return Pair(datePattern, folderName)
    }

    fun createDatefolders(datePattern:String): String {
        val currDateTime = LocalDateTime.now(ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern(datePattern)
        val formatDateTimeStr = currDateTime.format(formatter)
        return formatDateTimeStr

    }

}