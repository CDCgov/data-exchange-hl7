package com.example

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import com.google.gson.Gson 
import com.azure.storage.blob.*
import com.azure.storage.blob.models.*

import com.azure.messaging.eventhubs.*

import java.util.UUID
import java.io.*

const val BLOB_CREATED = "Microsoft.Storage.BlobCreated"
const val UTF_BOM = "\uFEFF"

/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    @FunctionName("ehprocessor001kt")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg", 
                eventHubName = "eventhub001", 
                connection = "EventHubConnectionString") 
                message: String?,
            context: ExecutionContext) {

        // context.logger.info("message: --> " + message)

        // 
        // Event Hub Sender
        // -------------------------------------
        val evHubName = System.getenv("EventHubSendOkName")
        val evHubConnStr = System.getenv("EventHubConnectionString")
        
        val evHubSender = EvHubSender(evHubName=evHubName, evHubConnStr=evHubConnStr)

        
        val blobIngestContName = System.getenv("BlobIngestContainerName")
        val ingestBlobConnStr = System.getenv("BlobIngestConnectionString")


        // 
        // Blob Container Client
        // -------------------------------------
        val blobContainerClient = BlobServiceClientBuilder()
                                    .connectionString(ingestBlobConnStr)
                                    .buildClient()
                                    .getBlobContainerClient(blobIngestContName)

        // 
        // Event Hub -> receive events
        // -------------------------------------
        val eventArrArr = Gson().fromJson(message, Array<Array<EvHubMessage>>::class.java)
        val eventArr = eventArrArr[0]

        // 
        // For each Event received
        // -------------------------------------
        for (event in eventArr) {

            if ( event.eventType == BLOB_CREATED) {
                
                context.logger.info( "event: --> " + event.toString() )

                // 
                // Pick up blob metadata
                // -------------------------------------
                val blobName = event.evHubData.url.split("/").last()
                val blobClient = blobContainerClient.getBlobClient(blobName)

                val blobContentType = blobClient.getProperties().getContentType()
                val blobLastModified = blobClient.getProperties().getLastModified()
                val blobSize = blobClient.getProperties().getBlobSize()

                // 
                // Create HL7 Message Metadata
                // -------------------------------------
                val hl7MessageMetadata = HL7MessageMetadata(
                    filePath=event.evHubData.url,
                    fileName=blobName,
                    fileTimestamp=blobLastModified.toString(),
                    fileSize=blobSize,
                    fileUUID= UUID.randomUUID().toString(),
                    fileEventID=event.id,
                    fileEventTimestamp=event.eventTime,
                ) // .hl7MessageMetadata

                // 
                // Read Blob File by Lines
                // -------------------------------------
                val reader = InputStreamReader( blobClient.openInputStream() )

                var index = 0
                context.logger.info("index: -----> " + index)
                // var hl7MessagesArr = arrayListOf<String>() 
                var currentLinesArr = arrayListOf<String>() 

                BufferedReader( reader ).use { br ->
                    var line: String?
                    while ( br.readLine().also { line = it } != null ) {
                        context.logger.info("line: ------> " + line)

                        var lineClean = line!!.trim()
                        if ( lineClean.startsWith(UTF_BOM) ) {
                            lineClean = lineClean.substring(1)
                        } // .if

                        if ( lineClean.startsWith("FHS") || lineClean.startsWith("BHS") || lineClean.startsWith("BTS") || lineClean.startsWith("BHS")  ) { 
                            // batch line 
                        } else {

                            if ( lineClean.startsWith("MSH") ) {
                                context.logger.info("line.startsWith(MSH): ------> " + line)
                                if ( index > 0 ) {
                                    // hl7MessagesArr.add( currentLinesArr.joinToString(separator="\n") )
                                    
                                    // 
                                    // Create and Send HL7 Message to next Event Hub
                                    // -------------------------------------
                                    hl7MessageMetadata.messageUUID = UUID.randomUUID().toString() 
                                    hl7MessageMetadata.messageIndex = index 

                                    val hl7Message = HL7Message(
                                        content = currentLinesArr.joinToString(separator="\n") ,
                                        metadata = hl7MessageMetadata,
                                    )
                                    context.logger.info("sending to event hub (ongoing): --> " + hl7Message.metadata.messageIndex + ", " + hl7Message.metadata.messageUUID)
                                    evHubSender.send(hl7Message=hl7Message)

                                } // .if 
                                index = index + 1
                                context.logger.info("index (+1): -----> " + index)
                                // empty for new message
                                currentLinesArr.clear()

                            } // .if 

                            currentLinesArr.add(lineClean)

                        } // .else

                    } // .while
                    
                    // Send last message
                    // 
                    // Create and Send HL7 Message to next Event Hub
                    // -------------------------------------
                    hl7MessageMetadata.messageUUID = UUID.randomUUID().toString() 
                    hl7MessageMetadata.messageIndex = index
                    val hl7Message = HL7Message(
                                        content = currentLinesArr.joinToString(separator="\n") ,
                                        metadata = hl7MessageMetadata,
                                    )
                    context.logger.info("sending to event hub (last): --> " + hl7Message.metadata.messageIndex + ", " + hl7Message.metadata.messageUUID)
                    evHubSender.send(hl7Message=hl7Message)
                    // hl7MessagesArr.add(currentLinesArr.joinToString(separator="\n") )

                } // .BufferedReader 


                // context.logger.info("size: --> " + hl7MessagesArr[0] )
            } // .if


        } // .for 

        // context.logger.info(message)

    } // .eventHubProcessor

} // .Function

