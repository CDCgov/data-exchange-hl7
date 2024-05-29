package gov.cdc.dex.hl7

import gov.cdc.dex.azure.AzureBlobProxy
import gov.cdc.dex.azure.DedicatedEventHubSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FunctionConfig {
    val azBlobProxy: AzureBlobProxy
    val evHubSenderOut: DedicatedEventHubSender
    val evHubSenderReports: DedicatedEventHubSender
    var attachmentBlobProxy: AzureBlobProxy
    val blobAttachmentContName = "hl7-attachments"
    private val logger : Logger = LoggerFactory.getLogger(this.javaClass.simpleName)
    val blobIngestContName: String = try {
        System.getenv("BlobIngestContainerName")
    } catch (e: NullPointerException) {
        logger.error("FATAL: Missing environment variable BlobIngestContainerName")
        throw e
    }
    val evHubSendName: String = try {
        System.getenv("EventHubSendName")
    } catch(e: NullPointerException) {
        logger.error("FATAL: Missing environment variable EventHubSendName")
        throw e
    }
    val evReportsHubName: String = try {
        System.getenv("EventReportsHubName")
    } catch (e: NullPointerException) {
        logger.error ("FATAL: Missing environment variable EventReportsHubName")
        throw e
    }

    init {
         //Init Azure Storage connection
        val ingestBlobConnStr = try {
            System.getenv("BlobIngestConnectionString")
        } catch (e: NullPointerException) {
            logger.error("FATAL: Missing environment variable BlobIngestConnectionString")
            throw e
        }
        val attachmentBlobConnStr = try {
            System.getenv("attachmentBlobConnString")
        } catch (e: NullPointerException) {
            logger.error("FATAL: Missing environment variable BlobIngestConnectionString")
            throw e
        }
        azBlobProxy = AzureBlobProxy(ingestBlobConnStr, blobIngestContName)
        val evHubConnStr = try {
            System.getenv("EventHubConnectionString")
        } catch (e: NullPointerException) {
            logger.error("FATAL: Missing environment variable EventHubConnectionString")
            throw e
        }
        attachmentBlobProxy = AzureBlobProxy(attachmentBlobConnStr, blobAttachmentContName)
        evHubSenderOut = DedicatedEventHubSender(evHubConnStr, evHubSendName)
        evHubSenderReports = DedicatedEventHubSender(evHubConnStr, evReportsHubName)

    }
}