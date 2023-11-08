package gov.cdc.dataexchange.util

import com.azure.core.util.polling.LongRunningOperationStatus
import com.azure.core.util.polling.SyncPoller
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobCopyInfo
import com.azure.storage.blob.models.BlobStorageException
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class BlobService {

    companion object {
        private const val SUCCESS = "SUCCESS"
        private const val NOT_FOUND = "NOT_FOUND"
        private const val FAILED_OPERATION = "FAILED_OPERATION"
        private val logger = LoggerFactory.getLogger(BlobService::class.java.simpleName)

        fun copyFile(
            srcContainerName: String,
            destContainerName: String,
            srcFolderPath: String,
            destFolderPath: String,
            filename: String,
            srcConnectionString: String,
            destConnectionString: String,
            n: Int?
        ): String {
            try {
                // initialize clients
                val srcBlobServiceClient = BlobServiceClientBuilder().connectionString(srcConnectionString).buildClient()
                val destBlobServiceClient = BlobServiceClientBuilder().connectionString(destConnectionString).buildClient()
                val srcContainerClient = srcBlobServiceClient.getBlobContainerClient(srcContainerName)
                val destContainerClient = destBlobServiceClient.getBlobContainerClient(destContainerName)

                val timesToCopy = n ?: 1 // if n is not provided, copy once
                for (i in 1..timesToCopy) {
                    // format source filename with path
                    val srcPath = if (srcFolderPath.isNotBlank()) "$srcFolderPath/" else srcFolderPath
                    val srcBlobName = "$srcPath$filename" // prepend virtual folder path
                    val srcBlobClient = srcContainerClient.getBlobClient(srcBlobName) // reference src blob

                    // validate
                    if (!srcBlobClient.exists()) {
                        logger.error("BLOB-SERVICE::Source blob does not exist: $srcBlobName")
                        return NOT_FOUND
                    }

                    // format destination filename and path
                    // prepend destination path and append timestamp before extension if n > 1
                    val destPath = if (destFolderPath.isNotBlank()) "$destFolderPath/" else destFolderPath
                    val filenameWithoutExtension = filename.substringBeforeLast(".", filename)
                    val filenameExtension = filename.substringAfterLast(".", "")
                    val destBlobName = "$destPath$filenameWithoutExtension" +
                            if (timesToCopy > 1) {
                                "-${DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now().plusSeconds(i.toLong()))}" +
                                        (if (filenameExtension.isNotBlank()) ".$filenameExtension" else "")
                            } else {
                                if (filenameExtension.isNotBlank()) ".$filenameExtension" else ""
                            }
                    // reference destination blob
                    val destBlobClient = destContainerClient.getBlobClient(destBlobName)

                    // copy blob
                    val poller: SyncPoller<BlobCopyInfo, Void> = destBlobClient.beginCopy(srcBlobClient.blobUrl, Duration.ofSeconds(1))
                    val pollResponse = poller.poll()
                    logger.info("BLOB-SERVICE::[$i] copy id: ${pollResponse.value.copyId}")
                    val copyResult = poller.waitForCompletion() // wait for the copy operation to complete

                    // copy metadata
                    if (copyResult.status == LongRunningOperationStatus.SUCCESSFULLY_COMPLETED) {
                        val metadata = srcBlobClient.properties.metadata
                        destBlobClient.setMetadata(metadata)
                    } else {
                        logger.error("BLOB-SERVICE::Copy operation failed for blob: $srcBlobName")
                        return FAILED_OPERATION
                    }
                }
                return SUCCESS // copy successful
            } catch (e: BlobStorageException) {
                logger.error("BLOB-SERVICE::Azure Storage Exception: ${e.message}")
                return e.serviceMessage
            } catch (e: Exception) {
                val message = "Exception occurred while copying the file: ${e.message}"
                logger.error("BLOB-SERVICE::$message")
                return message
            }
        }
    }
}
