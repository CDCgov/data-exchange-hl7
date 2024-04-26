package gov.cdc.dataexchange

import com.azure.identity.DefaultAzureCredentialBuilder
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess
import gov.cdc.dex.azure.AzureBlobProxy
class FunctionConfig {
    val blobStorageContainerName: String
    var azureBlobProxy: AzureBlobProxy
    val blobStorageFolderName: String
    private val blobStorageUri: String
    val blobUploadTimeout: String
    val blobUploadRetryDelay: String
    val blobUploadMaxRetries: String
    private val identityClientId: String

    private var logger = LoggerFactory.getLogger(FunctionConfig::class.java.simpleName)

    init {
        //Init Azure Storage connection
        try {
            blobStorageContainerName = System.getenv("BlobStorageContainerName")
        } catch (e : NullPointerException) {
            logger.error("ERROR: BlobStorageContainerName not provided")
            exitProcess(1)
        }
        try {
            blobStorageFolderName = System.getenv("BlobStorageFolderName")
        } catch (e: NullPointerException) {
            logger.error("ERROR: BlobStorageFolderName not provided")
            exitProcess(1)
        }
        try {
            identityClientId = System.getenv("BlobStorageClientId")
        } catch (e: NullPointerException) {
            logger.error("ERROR: BlobStorageClientId not provided")
            exitProcess(1)
        }

        try {
            blobStorageUri = System.getenv("BlobStorageUri")
        } catch (e: NullPointerException) {
            logger.error("ERROR: BlobStorageConnectionString not provided")
            exitProcess(1)
        }

        val tokenCredential = DefaultAzureCredentialBuilder()
            .managedIdentityClientId(identityClientId)
            .build()

        azureBlobProxy = AzureBlobProxy(blobStorageUri, blobStorageContainerName, tokenCredential)
        blobUploadTimeout = System.getenv("BlobUploadTimeoutSeconds") ?: "60"
        blobUploadRetryDelay = System.getenv("BlobUploadRetryDelaySeconds") ?: "10"
        blobUploadMaxRetries = System.getenv("BlobUploadMaxRetries") ?: "3"
    }

}
