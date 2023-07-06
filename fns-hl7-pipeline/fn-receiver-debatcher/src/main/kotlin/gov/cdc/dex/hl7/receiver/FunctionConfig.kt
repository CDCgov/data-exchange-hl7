package gov.cdc.dex.hl7.receiver

import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.mmg.MmgUtil

class FunctionConfig {

    val azBlobProxy:AzureBlobProxy
//    val redisProxy:RedisProxy
    val mmgUtil: MmgUtil
    val evHubSender: EventHubSender

    val evHubOkName: String = System.getenv("EventHubSendOkName")
    val evHubErrorName: String = System.getenv("EventHubSendErrsName")
    val azureBlobContainer: String = System.getenv("AZURE_BLOB_CONTAINER") ?: "hl7ingress"
    init {
         //Init Event Hub connections
         val evHubConnStr = System.getenv("EventHubConnectionString")
         evHubSender = EventHubSender(evHubConnStr)

         //Init Redis Connections
         val redisName: String = System.getenv("REDIS_CACHE_NAME")
         val redisKey: String = System.getenv("REDIS_CACHE_KEY")
         val redisProxy = RedisProxy(redisName, redisKey)
         mmgUtil = MmgUtil(redisProxy)

         //Init Azure Storage connection
         val blobIngestContName = System.getenv("BlobIngestContainerName")
         val ingestBlobConnStr = System.getenv("BlobIngestConnectionString")
         azBlobProxy = AzureBlobProxy(ingestBlobConnStr, blobIngestContName)
    }
}