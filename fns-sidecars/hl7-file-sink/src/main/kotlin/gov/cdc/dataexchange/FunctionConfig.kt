package gov.cdc.dataexchange

import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class FunctionConfig {
    val blobStorageContainerName: String
    var azureBlobProxy: AzureBlobProxy
    val blobStorageFolderName: String
    val blobStorageConnectionString: String
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
            blobStorageConnectionString = System.getenv("BlobStorageConnectionString")
            azureBlobProxy = AzureBlobProxy(blobStorageConnectionString, blobStorageContainerName)
        } catch (e: NullPointerException) {
            logger.error("ERROR: BlobStorageConnectionString not provided")
            exitProcess(1)
        }

    }
}
