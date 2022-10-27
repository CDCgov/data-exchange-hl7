package gov.cdc.dex.hl7.receiver

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder

class AzureBlobProxy(connectionStr: String, container: String) {
    private val blobContainerClient: BlobContainerClient = BlobServiceClientBuilder()
       .connectionString(connectionStr)
       .buildClient()
       .getBlobContainerClient(container)

    fun getBlobClient(blobName: String): BlobClient {
        return blobContainerClient.getBlobClient(blobName)
    }
}