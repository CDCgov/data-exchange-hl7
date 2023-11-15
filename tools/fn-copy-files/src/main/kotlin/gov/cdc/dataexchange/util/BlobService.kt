package gov.cdc.dataexchange.util

import com.azure.core.util.polling.LongRunningOperationStatus
import com.azure.core.util.polling.SyncPoller
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobCopyInfo
import com.azure.storage.blob.models.BlobStorageException
import com.azure.storage.blob.options.BlobBeginCopyOptions
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import com.azure.storage.blob.models.ListBlobsOptions

class BlobService {

    companion object {
        const val SUCCESS = "SUCCESS"
        const val NOT_FOUND = "NOT_FOUND"
        const val FAILED_OPERATION = "FAILED_OPERATION"
        private val logger = LoggerFactory.getLogger(BlobService::class.java.simpleName)

        fun copyStorage(
            srcContainerName: String,
            srcPath: String,
            destContainerName: String,
            destPath: String,
            connectionString: String,
            doCount: Boolean
        ): String {
            var totalRuntime: Long = 0
            logger.info("Initializing Clients...")
            try {
                val blobServiceClient = BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient()

                val srcContainerClient = blobServiceClient.getBlobContainerClient(srcContainerName)
                val destContainerClient = blobServiceClient.getBlobContainerClient(destContainerName)

                val srcBlobs =
                    srcContainerClient.listBlobs(ListBlobsOptions().setPrefix(srcPath), Duration.ofSeconds(30))

                // counting
                var count = 0
                var srcBlobCount: Int? = null
                if (doCount) {
                    logger.info("Counting...")
                    srcBlobCount = srcBlobs.count { srcBlobItem ->
                        !(srcContainerClient.getBlobClient(srcBlobItem.name)
                            .properties.metadata.containsKey("hdi_isfolder")
                                && srcContainerClient.getBlobClient(srcBlobItem.name)
                            .properties.metadata.getValue("hdi_isfolder") == "true")
                    }
                }

                //validate
                if (srcBlobCount == 0) {
                    logger.error("No blobs found at source.")
                    return NOT_FOUND
                } else if (srcBlobCount != null) {
                    logger.info("$srcBlobCount blobs found at ${srcContainerClient.blobContainerName}/$srcPath")
                }

                // copy each blob
                srcBlobs.forEach { srcBlobItem ->
                    // filter out folder blobs
                    if (srcContainerClient.getBlobClient(srcBlobItem.name)
                            .properties.metadata.containsKey("hdi_isfolder")
                                && srcContainerClient.getBlobClient(srcBlobItem.name)
                            .properties.metadata.getValue("hdi_isfolder") == "true") {
                        return@forEach
                    }
                    val startTime = System.currentTimeMillis()
                    var duration: Long = 0

                    // get source and destination clients
                    val srcBlob = srcContainerClient.getBlobClient(srcBlobItem.name)
                    val destBlobName = if (destPath.isNotBlank()) "$destPath/${srcBlobItem.name.substringAfter(srcPath)}"
                        else srcBlobItem.name.substringAfter(srcPath)
                    val destBlob = destContainerClient.getBlobClient(destBlobName)

                    // copy
                    val copyOpts = BlobBeginCopyOptions(srcBlob.blobUrl)
                    copyOpts.setMetadata(srcBlob.properties.metadata)
                    destBlob.beginCopy(copyOpts)

                    // performance insight
                    duration += System.currentTimeMillis() - startTime
                    totalRuntime += duration
                    count++
                    var percentage: Int? = null
                    if (doCount && srcBlobCount != null) {
                        percentage = (((count).toDouble() / srcBlobCount.toDouble()) * 100).toInt()
                    }
                    val countDisplay = if (doCount) "${count}/$srcBlobCount" else "${count}"
                    logger.info("**** Copy blob ($countDisplay) ****")
                    logger.info("${blobServiceClient.accountUrl}/$srcContainerName/${srcBlobItem.name} " +
                                "is copying to $destContainerName/$destPath")
                    if (logger.isDebugEnabled) {
                        logger.debug("Metadata: {}", srcBlob.properties.metadata)
                    }
                    logger.info("Duration: ${duration.getRuntimeString()}, total runtime: ${totalRuntime.getRuntimeString()}")
                    if (doCount && percentage != null) {
                        logger.info("${percentage}% of blobs are copying successfully.")
                    }
                }
                // check load balance
                if (doCount && srcBlobCount != null && count != srcBlobCount) {
                    val missingCount = srcBlobCount.minus(count)
                    logger.error("ERROR - Unbalanced Count.  $missingCount missing blobs")
                }

                // log success
                logger.info("**** SUCCESS::$count blobs copied in ${totalRuntime.getRuntimeString()} ****")
                return SUCCESS
            } catch (e: BlobStorageException) {
                logger.error("ERROR: Azure Storage Exception: ${e.message}")
                return e.serviceMessage
            } catch (e: Exception) {
                val message = "ERROR: Exception occurred while copying blob: ${e.message}"
                logger.error(message)
                return message
            }
        }

        fun copyFile(
            srcContainerName: String,
            srcFolderPath: String,
            destContainerName: String,
            destFolderPath: String,
            filename: String,
            srcConnectionString: String,
            destConnectionString: String,
            n: Int?
        ): String {
            try {
                // initialize clients
                val srcBlobServiceClient =
                    BlobServiceClientBuilder().connectionString(srcConnectionString).buildClient()
                val destBlobServiceClient =
                    BlobServiceClientBuilder().connectionString(destConnectionString).buildClient()
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
                                "-${
                                    DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                                        OffsetDateTime.now().plusSeconds(i.toLong())
                                    )
                                }" +
                                        (if (filenameExtension.isNotBlank()) ".$filenameExtension" else "")
                            } else {
                                if (filenameExtension.isNotBlank()) ".$filenameExtension" else ""
                            }
                    // reference destination blob
                    val destBlobClient = destContainerClient.getBlobClient(destBlobName)

                    // copy blob
                    val poller: SyncPoller<BlobCopyInfo, Void> =
                        destBlobClient.beginCopy(srcBlobClient.blobUrl, Duration.ofSeconds(1))
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

        private fun Long.getRuntimeString(): String {
            if (this < 1000) {
                return "$this ms"
            }
            val seconds = this / 1000
            val remainingMs = this % 1000
            if (seconds < 60) {
                if(seconds < 10) {
                    return "$seconds.$remainingMs seconds"
                }
                return "$seconds seconds"
            }
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            if (minutes < 60) {
                return "$minutes min $remainingSeconds sec"
            }
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            return "$hours hours $remainingMinutes min $remainingSeconds sec"
        }
    }
}