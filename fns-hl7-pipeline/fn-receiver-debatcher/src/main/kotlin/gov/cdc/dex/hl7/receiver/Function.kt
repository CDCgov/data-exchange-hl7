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
import java.io.*
import java.util.*


/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    companion object {
        const val BLOB_CREATED = "Microsoft.Storage.BlobCreated"
        const val UTF_BOM = "\uFEFF"
        val gson = Gson()
    }
    @FunctionName("receiverdebatcher001")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveName%",
                consumerGroup = "%EventHubConsumerGroup%",
                connection = "EventHubConnectionString") 
                message: String?,
            context: ExecutionContext) {

        // context.logger.info("message: --> " + message)

        // 
        // Event Hub Sender
        // -------------------------------------
        val evHubName = System.getenv("EventHubSendOkName")
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val blobIngestContName = System.getenv("BlobIngestContainerName")
        val ingestBlobConnStr = System.getenv("BlobIngestConnectionString")

        val evHubSender = EventHubSender( evHubConnStr=evHubConnStr)
        val azBlobProxy = AzureBlobProxy(ingestBlobConnStr, blobIngestContName)

        // 
        // Event Hub -> receive events
        // -------------------------------------
        val eventArrArr = Gson().fromJson(message, Array<Array<AzBlobCreateEventMessage>>::class.java)
        val eventArr = eventArrArr[0]

        // 
        // For each Event received
        // -------------------------------------
        for (event in eventArr) {
            if ( event.eventType == BLOB_CREATED) {
                context.logger.info("Received BLOB_CREATED event: --> $event")
                //
                // Pick up blob metadata
                // -------------------------------------
                val blobName = event.evHubData.url.split("/").last()
                val blobClient = azBlobProxy.getBlobClient(blobName)
                //
                // Create HL7 Message Metadata for Provenance
                // -------------------------------------
                val provenance = Provenance(
                    filePath=event.evHubData.url,
                    fileTimestamp=blobClient.properties.lastModified.toIsoString(),
                    fileSize=blobClient.properties.blobSize,
                    singleOrBatch=Provenance.SINGLE_FILE,
                    eventId=event.id,
                    eventTimestamp=event.eventTime,
                    originalFileName =blobName,
                    systemProvider = "BLOB"
                ) // .hl7MessageMetadata
                val summary = SummaryInfo("RECEIVED")
                val processMD = ReceiverProcessMetadata(ProcessMetadata.STATUS_COMPLETE)
                val metadata = DexMetadata(provenance, listOf(processMD))

                // Read Blob File by Lines
                // -------------------------------------
                val reader = InputStreamReader( blobClient.openInputStream(), Charsets.UTF_8 )
                val currentLinesArr = arrayListOf<String>()

                BufferedReader(reader).use { br ->
                    br.forEachLine {line ->
                        val lineClean = line.trim().let { if ( it.startsWith(UTF_BOM) )  it.substring(1)  else it}
                        if ( lineClean.startsWith("FHS") || lineClean.startsWith("BHS") || lineClean.startsWith("BTS") || lineClean.startsWith("BHS")  ) {
                            // batch line --Nothing to do here
                            provenance.singleOrBatch = Provenance.BATCH_FILE
                        } else {
                            if ( lineClean.startsWith("MSH") ) {
                                // context.logger.info("line.startsWith(MSH): ------> " + line)
                                if ( provenance.messageIndex > 1 ) {
                                    provenance.singleOrBatch = Provenance.BATCH_FILE
                                    prepareAndSend(currentLinesArr, metadata, summary, evHubSender, evHubName, context)
                                    provenance.messageIndex++
                                } // .if 
                                currentLinesArr.clear()

                            } // .if 
                            currentLinesArr.add(lineClean)
                        } // .else
                    } // .forEachLine
                    // Send last message
                    prepareAndSend(currentLinesArr, metadata, summary, evHubSender, evHubName, context)
                } // .BufferedReader
            } // .if
        } // .for
    } // .eventHubProcessor

    private fun prepareAndSend(messageContent: ArrayList<String>, metadata: DexMetadata, summary: SummaryInfo, eventHubSender: EventHubSender, eventHubName: String, context: ExecutionContext) {
        val contentBase64 = Base64.getEncoder().encodeToString(messageContent.joinToString(separator="\n").toByteArray())
        val msgEvent = DexEventPayload(contentBase64, metadata, summary)

        val jsonMessage = gson.toJson(msgEvent)
        eventHubSender.send(eventHubName, jsonMessage)
        context.logger.info("Processed and Sent to event hub Message: --> messageIndex: ${msgEvent.metadata.provenance.messageIndex}, messageUUID: ${msgEvent.messageUUID}, fileName: ${msgEvent.metadata.provenance.filePath}")

    }
} // .class  Function

