package gov.cdc.dex.hl7.receiver

import com.azure.messaging.eventhubs.*
import com.azure.storage.blob.*
import com.azure.storage.blob.models.*
import com.google.gson.Gson
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.metadata.*
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.StringUtils.Companion.hashMD5
import java.io.*
import java.util.*


/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    companion object {
        const val BLOB_CREATED = "Microsoft.Storage.BlobCreated"
        const val UTF_BOM = "\uFEFF"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_ERROR = "ERROR"
        val gson = Gson()
    }
    @FunctionName("receiverdebatcher001")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveName%",
                consumerGroup = "%EventHubConsumerGroup%",
                connection = "EventHubConnectionString") 
                messages: List<String>?,
            context: ExecutionContext) {

        val startTime = Date().toIsoString()
        // context.logger.info("message: --> " + message)
        val evHubName = System.getenv("EventHubSendOkName")
        val evHubErrsName = System.getenv("EventHubSendErrsName")
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val blobIngestContName = System.getenv("BlobIngestContainerName")
        val ingestBlobConnStr = System.getenv("BlobIngestConnectionString")

        val evHubSender = EventHubSender(evHubConnStr)
        val azBlobProxy = AzureBlobProxy(ingestBlobConnStr, blobIngestContName)

        if (messages != null) {
            for (message in messages) {
                val eventArr = Gson().fromJson(message, Array<AzBlobCreateEventMessage>::class.java)
                val event = eventArr[0]
                if ( event.eventType == BLOB_CREATED) {
                    context.logger.info("Received BLOB_CREATED event: --> $event")
                    // Pick up blob metadata
                    val blobName = event.evHubData.url.split("/").last()
                    context.logger.fine("Reading blob $blobName")
                    val blobClient = azBlobProxy.getBlobClient(blobName)
                    // Create Metadata for Provenance
                    val provenance = Provenance(
                        eventId=event.id,
                        eventTimestamp=event.eventTime,
                        filePath=event.evHubData.url,
                        fileTimestamp=blobClient.properties.lastModified.toIsoString(),
                        fileSize=blobClient.properties.blobSize,
                        singleOrBatch=Provenance.SINGLE_FILE,
                        originalFileName =blobName,
                        systemProvider = "BLOB"
                    ) // .hl7MessageMetadata

                    // Read Blob File by Lines
                    // -------------------------------------
                    val reader = InputStreamReader( blobClient.openInputStream(), Charsets.UTF_8 )
                    val currentLinesArr = arrayListOf<String>()
                    var mshCount = 0
                    BufferedReader(reader).use { br ->
                        br.forEachLine { line ->
                            val lineClean = line.trim().let { if ( it.startsWith(UTF_BOM) )  it.substring(1)  else it}
                            if ( lineClean.startsWith("FHS") || lineClean.startsWith("BHS") || lineClean.startsWith("BTS")  ) {
                                // batch line --Nothing to do here
                                provenance.singleOrBatch = Provenance.BATCH_FILE
                            } else {
                                if ( lineClean.startsWith("MSH") ) {
                                    mshCount++
                                    if ( mshCount > 1 ) {
                                        provenance.singleOrBatch = Provenance.BATCH_FILE
                                        provenance.messageHash = currentLinesArr.joinToString("\n").hashMD5()
                                        val (metadata, summary) = buildMetadata(STATUS_SUCCESS, startTime, provenance)
                                        prepareAndSend(currentLinesArr, metadata, summary, evHubSender, evHubName, context)
                                        provenance.messageIndex++
                                    }
                                    currentLinesArr.clear()
                                } // .if
                                currentLinesArr.add(lineClean)
                            } // .else
                        } // .forEachLine
                    } // .BufferedReader
                    // Send last message
                    provenance.messageHash = currentLinesArr.joinToString("\n").hashMD5()
                    if (mshCount > 0) {
                        val (metadata, summary) = buildMetadata(STATUS_SUCCESS, startTime, provenance)
                        prepareAndSend(currentLinesArr, metadata, summary, evHubSender, evHubName, context)
                    } else {
                        // no valid message -- send to error queue
                        val (metadata, summary) = buildMetadata(STATUS_ERROR, startTime, provenance, "No valid message found.")
                        prepareAndSend(currentLinesArr, metadata, summary, evHubSender, evHubErrsName, context)
                    }
                } // .if
            }
        } // .for
    } // .eventHubProcess

    private fun buildMetadata (status: String, startTime: String, provenance: Provenance, errorMessage: String? = null) : Pair<DexMetadata, SummaryInfo> {
        val processMD = ReceiverProcessMetadata(status)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()
        val summary = SummaryInfo("RECEIVED")
        if (!errorMessage.isNullOrEmpty() ) {
            summary.problem= Problem(ReceiverProcessMetadata.RECEIVER_PROCESS, null, null, errorMessage, false, 0, 0)
        }
        return DexMetadata(provenance, listOf(processMD)) to summary
    }

    private fun prepareAndSend(messageContent: ArrayList<String>, metadata: DexMetadata, summary: SummaryInfo, eventHubSender: EventHubSender, eventHubName: String, context: ExecutionContext) {
        val contentBase64 = Base64.getEncoder().encodeToString(messageContent.joinToString(separator="\n").toByteArray())
        val msgEvent = DexEventPayload(contentBase64, metadata, summary)
        context.logger.info("Sending new Event to event hub Message: --> messageUUID: ${msgEvent.messageUUID}, messageIndex: ${msgEvent.metadata.provenance.messageIndex}, fileName: ${msgEvent.metadata.provenance.filePath}")
        val jsonMessage = gson.toJson(msgEvent)
        eventHubSender.send(evHubTopicName=eventHubName, message=jsonMessage)
        context.logger.info("full message: $jsonMessage")
        context.logger.info("Processed and Sent to event hub $eventHubName Message: --> messageUUID: ${msgEvent.messageUUID}, messageIndex: ${msgEvent.metadata.provenance.messageIndex}, fileName: ${msgEvent.metadata.provenance.filePath}")
        println(msgEvent)
    }
} // .class  Function

