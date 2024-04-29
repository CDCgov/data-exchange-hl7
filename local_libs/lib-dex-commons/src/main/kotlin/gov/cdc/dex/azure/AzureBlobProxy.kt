package gov.cdc.dex.azure


import com.azure.core.credential.TokenCredential
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder

class AzureBlobProxy(blobStorageUrl: String, container: String, tokenCredential: TokenCredential) {
    private val blobContainerClient: BlobContainerClient

    init {
        blobContainerClient = BlobServiceClientBuilder()
            .endpoint(blobStorageUrl)
            .credential(tokenCredential)
            .buildClient()
            .getBlobContainerClient(container)
    }
    fun getBlobClient(blobName: String): BlobClient {
        return blobContainerClient.getBlobClient(blobName)
    }


}