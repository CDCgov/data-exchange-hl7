package gov.cdc.dex.hl7

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.common.policy.RequestRetryOptions
import com.azure.storage.common.policy.RetryPolicyType
import java.time.Duration

class AzureBlobProxy(connectionStr: String, container: String) {
    val maxRetries = 3
    val tryTimeout = Duration.ofSeconds(2)
    val retryDelay = Duration.ofSeconds(2)
    val maxRetryDelay = Duration.ofSeconds(30)

    private val retryOptions = RequestRetryOptions(
        RetryPolicyType.EXPONENTIAL, maxRetries, tryTimeout, retryDelay, maxRetryDelay, "")

    private val blobContainerClient: BlobContainerClient = BlobServiceClientBuilder()
       .connectionString(connectionStr)
       .retryOptions(retryOptions)
       .buildClient()
       .getBlobContainerClient(container)

    fun getBlobClient(blobName: String): BlobClient {
        return blobContainerClient.getBlobClient(blobName)
    }
}