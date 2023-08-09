package com.azure.storage.blob

//import com.azure.core.util.Context
import com.azure.storage.blob.models.BlobProperties
//import com.azure.storage.blob.models.BlobRequestConditions
import com.azure.storage.blob.specialized.BlobInputStream
import java.io.File
//import java.io.InputStream
//import java.io.InputStreamReader
//import java.time.Duration

class BlobClient(val properties: BlobProperties) {
    fun openInputStream(): BlobInputStream {
        val blobPath = "${System.getenv("BlobIngestConnectionString")}/${System.getenv("BlobIngestContainerName")}/"
        return BlobInputStream(File("$blobPath/${properties.blobName}"))
    }
}