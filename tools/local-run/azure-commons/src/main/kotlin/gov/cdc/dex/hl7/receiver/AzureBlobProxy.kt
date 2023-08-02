package gov.cdc.dex.hl7.receiver

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.models.BlobProperties
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import util.parseFileName
import java.time.ZoneOffset
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.readText

class AzureBlobProxy(var connectionStr: String, var container: String) {
    companion object {
         val gson = GsonBuilder().serializeNulls().create()!!
    }

    fun getBlobClient(blobName: String): BlobClient {
        val fileNameAndExt = parseFileName(blobName)
        val blobPath = "${System.getenv("BlobIngestConnectionString")}/${System.getenv("BlobIngestContainerName")}/"

        val propsAsText  = Path("$blobPath/${fileNameAndExt.first}.properties")
            .readText()
        val props = JsonParser.parseString(propsAsText).asJsonObject
        val metadata = gson.fromJson(props.getAsJsonObject("metadata"), Map::class.java)
        val blobSize = props.get("blob_size").asLong
        val lastModified = Date().toInstant().atOffset(ZoneOffset.UTC)

        val properties = BlobProperties(metadata as Map<String,String>, lastModified, blobSize, blobName)
        return BlobClient(properties)
    }
}