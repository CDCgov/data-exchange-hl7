package gov.cdc.dex.hl7.receiver

import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.util.PathUtils

class FunctionConfig {

    val azBlobProxy:AzureBlobProxy
    val evHubSender: EventHubSender
    val eventCodes = mutableMapOf<String, Pair<String, String>>()

    val evHubOkName: String = System.getenv("EventHubSendOkName")
    val evHubErrorName: String = System.getenv("EventHubSendErrsName")
    val blobIngestContName = System.getenv("BlobIngestContainerName")
    init {
         //Init Event Hub connections
         val evHubConnStr = System.getenv("EventHubConnectionString")
         evHubSender = EventHubSender(evHubConnStr)

         //Init Azure Storage connection
         val ingestBlobConnStr = System.getenv("BlobIngestConnectionString")
         azBlobProxy = AzureBlobProxy(ingestBlobConnStr, blobIngestContName)

        //Load Event Codes
        eventCodes = loadEventCodes("event_codes")
    }

    private fun loadEventCodes(directory: String) : Map<String, Pair<String,String>> {
        val dir = PathUtils.getResourcePath(directory)
    }
}