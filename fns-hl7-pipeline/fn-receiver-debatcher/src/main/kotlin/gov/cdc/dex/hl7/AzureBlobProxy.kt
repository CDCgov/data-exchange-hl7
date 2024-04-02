package gov.cdc.dex.hl7

import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.common.policy.RequestRetryOptions
import com.azure.storage.common.policy.RetryPolicyType
import java.time.Duration

class AzureBlobProxy(connectionStr: String, container: String) {
    val maxRetries = try {
        System.getenv("MaxRetries").toInt()
    } catch (e: Exception) {
        3
    }
    val tryTimeout = try {
        val t = System.getenv("TryTimeoutSeconds").toLong()
        Duration.ofSeconds(t)
    } catch (e: Exception) {
        Duration.ofSeconds(2)
    }
    val retryDelay = try {
        val t = System.getenv("RetryDelaySeconds").toLong()
        Duration.ofSeconds(t)
    } catch (e: Exception) {
        Duration.ofSeconds(2)
    }
    val maxRetryDelay = try {
        val t = System.getenv("MaxRetryDelaySeconds").toLong()
        Duration.ofSeconds(t)
    } catch (e: Exception) {
        Duration.ofSeconds(300)
    }

    private val retryOptions = RequestRetryOptions(
        RetryPolicyType.EXPONENTIAL, maxRetries, tryTimeout, retryDelay, maxRetryDelay, "")

    private val blobContainerClient: BlobContainerClient = BlobServiceClientBuilder()
       .connectionString(connectionStr)
            // try it without the sdk retry options
     //  .retryOptions(retryOptions)
       .buildClient()
       .getBlobContainerClient(container)

    fun getBlobClient(blobName: String): BlobClient {
        return blobContainerClient.getBlobClient(blobName)
    }
}