package gov.cdc.dataexchange

import com.azure.core.util.BinaryData
import com.azure.storage.blob.models.AccessTier
import com.azure.storage.blob.models.BlobHttpHeaders
import com.google.gson.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.UnknownPropertyError
import io.netty.channel.unix.Errors.NativeIoException
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit


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
            connection = "EventHubConnection",
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
            val dataStreamId = try {
                JsonHelper.getValueFromJson("routing_metadata.data_stream_id", inputEvent).asString
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
                val supportingMeta = try {
                    JsonHelper.getValueFromJson("supporting_metadata", routingMetadata)
                } catch (e: Exception) {
                    JsonNull.INSTANCE
                }

                // update data_stream_route to match destination folder name
                routingMetadata.addProperty("data_stream_route", blobStorageFolderName)

                // add all routing metadata json elements to blob metadata we will attach on upload
                routingMetadata.keySet().forEach { key ->
                    if (!routingMetadata[key].isJsonNull && key != "supporting_metadata") {
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
               val dateStructure = createDateFolders(datePattern)
                logger.info("DEX::dateStructure $dateStructure")
               // save to storage container
               val succeeded =  this.saveBlobToContainer(
                    "$blobStorageFolderName/$dataStreamId/$dateStructure/$newBlobName.txt",
                    gson.toJson(inputEvent),
                    metaToAttach
                )
                if (succeeded) {
                    logger.info("DEX::Saved message $newBlobName.txt to sink ${fnConfig.blobStorageContainerName}/$blobStorageFolderName/$dateStructure")
                } else {
                    logger.error("DEX::ERROR: Unable to save message $newBlobName.txt to sink ${fnConfig.blobStorageContainerName}/$blobStorageFolderName/$dateStructure")
                }
            } catch (e: Exception) {
                // TODO send to quarantine?
                logger.error("DEX::Error processing message", e)
                throw Exception("Failure in file sink :${e.message}")
            }
        }

    }

    fun saveBlobToContainer(blobName: String, blobContent: String, newMetadata: MutableMap<String, String>) : Boolean {
        val data = BinaryData.fromString(blobContent)
        val length = data.length
        val md5 = MessageDigest.getInstance("MD5").digest(blobContent.toByteArray(StandardCharsets.UTF_8))
        val headers = BlobHttpHeaders()
            .setContentMd5(md5)
            .setContentLanguage("en-US")
            .setContentType("binary")

        var timeToWait = fnConfig.blobUploadRetryDelay.toLong()
        val timeout = fnConfig.blobUploadTimeout.toLong()
        var mustRetry: Boolean
        var retries = fnConfig.blobUploadMaxRetries.toInt()
        do {
            if (retries < 3) logger.info("RETRYING upload of blob $blobName")
            mustRetry = try {
                val client = fnConfig.azureBlobProxy.getBlobClient(blobName).blockBlobClient
                val dataStream = data.toStream()
                val response = client.uploadWithResponse(
                    dataStream, length, headers, newMetadata, AccessTier.HOT, md5,
                    null, Duration.ofSeconds(timeout), null
                )
                (response.statusCode !in listOf(200, 201))
            } catch (ex: NativeIoException) {
                logger.error("Error in connection: ${ex.message}")
                // reestablish connection
                fnConfig.connectAzureBlobProxy()
                true
            } catch (e: Exception) {
                logger.error("ERROR in saveBlobToContainer: ${e.javaClass.canonicalName}: ${e.message}")
                true
            }
            retries--
            if (mustRetry) {
                try {
                    TimeUnit.SECONDS.sleep(timeToWait++)
                } catch (ex: InterruptedException) {
                    logger.debug("Timer interrupted")
                }
            }

        } while (mustRetry && retries > 0)

        return !mustRetry
    }

    private fun getFolderDate(folder:String): Pair<String,String> {
        val splitPattern = folder.split("/")
        val datePattern = splitPattern.drop(1).joinToString("/")
        val folderName = splitPattern.first()
        return Pair(datePattern, folderName)
    }

    private fun createDateFolders(datePattern: String): String {
        val currDateTime = LocalDateTime.now(ZoneOffset.UTC)
        val formatter = DateTimeFormatter.ofPattern(datePattern)
        return currDateTime.format(formatter)

    }

}