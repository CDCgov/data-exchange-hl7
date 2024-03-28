package gov.cdc.dex.azure

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.StorageAccountInfo

class AzureBlobProxy(connectionStr: String, container: String) {
    private val blobServiceClient = BlobServiceClientBuilder()
            .connectionString(connectionStr)
            .buildClient()

    private val blobContainerClient : BlobContainerClient =
        blobServiceClient.getBlobContainerClient(container)

    fun getBlobClient(blobName: String): BlobClient {
        return blobContainerClient.getBlobClient(blobName)
    }

    fun getAccountInfo() : StorageAccountInfo {
        return blobServiceClient.accountInfo
    }

}