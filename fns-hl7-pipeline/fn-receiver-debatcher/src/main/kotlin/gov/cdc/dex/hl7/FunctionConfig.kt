package gov.cdc.dex.hl7

import com.azure.identity.DefaultAzureCredential
import com.azure.identity.DefaultAzureCredentialBuilder
import gov.cdc.dex.azure.AzureBlobProxy
import gov.cdc.dex.azure.DedicatedEventHubSender
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FunctionConfig {
    val azBlobProxy: AzureBlobProxy
    val evHubSenderOut: DedicatedEventHubSender
    val evHubSenderReports: DedicatedEventHubSender
    private val tokenCredential : DefaultAzureCredential = DefaultAzureCredentialBuilder().build()
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
        val ingestBlobUrl = try {
            System.getenv("BlobIngestUrl")
        } catch (e: NullPointerException) {
            logger.error("FATAL: Missing environment variable BlobIngestUrl")
            throw e
        }
        azBlobProxy = AzureBlobProxy(ingestBlobUrl, blobIngestContName, tokenCredential)
        val evHubNamespace = try {
            System.getenv("EventHubNamespace")
        } catch (e: NullPointerException) {
            logger.error("FATAL: Missing environment variable EventHubNamespace")
            throw e
        }
        evHubSenderOut = DedicatedEventHubSender(evHubNamespace, evHubSendName, tokenCredential)
        evHubSenderReports = DedicatedEventHubSender(evHubNamespace, evReportsHubName, tokenCredential)

    }
}