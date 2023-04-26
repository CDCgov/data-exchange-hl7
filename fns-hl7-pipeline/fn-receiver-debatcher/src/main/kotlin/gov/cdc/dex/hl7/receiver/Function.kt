package gov.cdc.dex.hl7.receiver

import com.azure.messaging.eventhubs.*
import com.azure.storage.blob.*
import com.azure.storage.blob.models.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.metadata.*
import gov.cdc.dex.mmg.InvalidConditionException
import gov.cdc.dex.mmg.MmgUtil
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.StringUtils.Companion.hashMD5
import gov.cdc.dex.util.StringUtils.Companion.normalize
import gov.cdc.hl7.HL7StaticParser
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
        const val MSH_9_PATH = "MSH-9"
        const val MSH_21_1_1_PATH = "MSH-21[1].1"
        const val MSH_21_2_1_PATH = "MSH-21[2].1" // Generic or Arbo
        const val MSH_21_3_1_PATH = "MSH-21[3].1" // Condition
        const val EVENT_CODE_PATH = "OBR-31.1"
        const val JURISDICTION_CODE_PATH = "OBX[@3.1='77968-6']-5.1"
        const val ALT_JURISDICTION_CODE_PATH = "OBX[@3.1='NOT116']-5.1"
        const val MAX_MESSAGE_SIZE = 1000000
        val gson: Gson = GsonBuilder().serializeNulls().create()
    }
    @FunctionName("receiverdebatcher001")
    fun eventHubProcessor(
        @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveName%",
                consumerGroup = "%EventHubConsumerGroup%",
                connection = "EventHubConnectionString") 
                messages: List<String>?,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext) {

        val startTime = Date().toIsoString()
        // context.logger.info("message: --> " + message)
        val evHubName = System.getenv("EventHubSendOkName")
        val evHubErrsName = System.getenv("EventHubSendErrsName")
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val blobIngestContName = System.getenv("BlobIngestContainerName")
        val ingestBlobConnStr = System.getenv("BlobIngestConnectionString")
        val redisName: String = System.getenv("REDIS_CACHE_NAME")
        val redisKey: String = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(redisName, redisKey)
        val mmgUtil = MmgUtil(redisProxy)
        val evHubSender = EventHubSender(evHubConnStr)
        val azBlobProxy = AzureBlobProxy(ingestBlobConnStr, blobIngestContName)

        if (messages != null) {
            var nbrOfMessages = 0
            for (message in messages) {
                val eventArr = gson.fromJson(message, Array<AzBlobCreateEventMessage>::class.java)
                val event = eventArr[0]
                if ( event.eventType == BLOB_CREATED) {
                    context.logger.info("Received BLOB_CREATED event: --> $event")
                    // Pick up blob metadata
                    val blobName = event.evHubData.url.split("/").last()
                    context.logger.fine("Reading blob $blobName")
                    val blobClient = azBlobProxy.getBlobClient(blobName)
                    //Create Map of Metadata with lower case keys
                    val metaDataMap = blobClient.properties.metadata.mapKeys { it.key.lowercase() }

                    // Create Metadata for Provenance
                    val provenance = Provenance(
                        eventId=event.id,
                        eventTimestamp=event.eventTime,
                        filePath=event.evHubData.url,
                        fileTimestamp=blobClient.properties.lastModified.toIsoString(),
                        fileSize=blobClient.properties.blobSize,
                        singleOrBatch=Provenance.SINGLE_FILE,
                        originalFileName =blobName,
                        systemProvider = metaDataMap["system_provider"],
                        originalFileTimestamp = metaDataMap["original_file_timestamp"]
                    ) // .hl7MessageMetadata
                    //Validate metadata
                    val isValidMessage = validateMessageMetaData(metaDataMap, context);

                    var messageType = metaDataMap["message_type"];

                    if(messageType.isNullOrEmpty()){
                        messageType = HL7MessageType.UNKOWN.name;
                       }

                    if(!isValidMessage){
                        // required Metadata is missing -- send to error queue
                        val (metadata, summary) = buildMetadata(STATUS_ERROR, eventHubMD[nbrOfMessages], startTime, provenance, "Message missing required Meta Data.")
                        // send empty array as message content when content is invalid
                        //Put Unknown as message type if messageType is missing else use messageType
                        prepareAndSend(arrayListOf(), DexMessageInfo(null, null, null, null, HL7MessageType.valueOf(messageType)), metadata, summary, evHubSender, evHubErrsName, context)
                        return
                    }

                    // Read Blob File by Lines
                    // -------------------------------------
                    val reader = InputStreamReader( blobClient.openInputStream(), Charsets.UTF_8 )
                    val currentLinesArr = arrayListOf<String>()
                    var mshCount = 0
                    BufferedReader(reader).use { br ->
                        br.forEachLine { line ->
                            val lineClean = line.trim().let { if ( it.startsWith(UTF_BOM) )  it.substring(1)  else it}
                            if ( lineClean.startsWith("FHS") || lineClean.startsWith("BHS") || lineClean.startsWith("BTS") || lineClean.startsWith(("FTS")) ) {
                                // batch line --Nothing to do here
                                provenance.singleOrBatch = Provenance.BATCH_FILE
                            } else {
                                if ( lineClean.startsWith("MSH") ) {
                                    mshCount++
                                    if ( mshCount > 1 ) {
                                        provenance.singleOrBatch = Provenance.BATCH_FILE
                                        provenance.messageHash = currentLinesArr.joinToString("\n").hashMD5()
                                        val messageInfo = getMessageInfo(metaDataMap, mmgUtil, currentLinesArr.joinToString("\n" ))
                                        val (metadata, summary) = buildMetadata(STATUS_SUCCESS, eventHubMD[nbrOfMessages], startTime, provenance)
                                        prepareAndSend(currentLinesArr, messageInfo, metadata, summary, evHubSender, evHubName, context)
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
                        val (metadata, summary) = buildMetadata(STATUS_SUCCESS, eventHubMD[nbrOfMessages], startTime, provenance)
                        val messageInfo = getMessageInfo(metaDataMap, mmgUtil, currentLinesArr.joinToString("\n" ))
                        prepareAndSend(currentLinesArr, messageInfo, metadata, summary, evHubSender, evHubName, context)
                    } else {
                        // no valid message -- send to error queue
                        val (metadata, summary) = buildMetadata(STATUS_ERROR, eventHubMD[nbrOfMessages], startTime, provenance, "No valid message found.")
                        // send empty array as message content when content is invalid
                        prepareAndSend(arrayListOf(), DexMessageInfo(null, null, null, null, HL7MessageType.valueOf(messageType)), metadata, summary, evHubSender, evHubErrsName, context)
                    }
                } // .if
             nbrOfMessages++
            }
        } // .for
    } // .eventHubProcess

    private fun getMessageInfo(metaDataMap: Map<String, String>, mmgUtil: MmgUtil, message: String): DexMessageInfo {
        val eventCode = extractValue(message, EVENT_CODE_PATH)

        //READ FROM METADATA FOR ELR
        val messageType = metaDataMap["message_type"];
        if(messageType == HL7MessageType.ELR.name){
            val route = metaDataMap["route"]?.normalize();
            val reportingJurisdiction = metaDataMap["reporting_jurisdiction"];
            return DexMessageInfo(eventCode, route, null,  reportingJurisdiction, HL7MessageType.ELR)
        }

        val msh21Gen = extractValue(message, MSH_21_2_1_PATH)
        val msh21Cond = extractValue(message, MSH_21_3_1_PATH)

        var jurisdictionCode = extractValue(message, JURISDICTION_CODE_PATH)
        if (jurisdictionCode.isEmpty()) {
            jurisdictionCode = extractValue(message, ALT_JURISDICTION_CODE_PATH)
        }

        return try {
            mmgUtil.getMMGMessageInfo(msh21Gen, msh21Cond, eventCode, jurisdictionCode)
        } catch (e : InvalidConditionException) {
            DexMessageInfo(eventCode, null, null,  jurisdictionCode, HL7MessageType.CASE)
        }

    }

    private fun extractValue(msg: String, path: String): String  {
        val value = HL7StaticParser.getFirstValue(msg, path)
        return if (value.isDefined) value.get()
        else ""
    }

    private fun buildMetadata (status: String, eventHubMD: EventHubMetadata, startTime: String, provenance: Provenance, errorMessage: String? = null) : Pair<DexMetadata, SummaryInfo> {
        val processMD = ReceiverProcessMetadata(status, eventHubMD)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()
        var summary = SummaryInfo("RECEIVED")
        if (status == STATUS_ERROR) {
            summary = SummaryInfo("REJECTED")
            summary.problem= Problem(ReceiverProcessMetadata.RECEIVER_PROCESS, null, null, errorMessage, false, 0, 0)
        }
        return DexMetadata(provenance, listOf(processMD)) to summary
    }

    private fun prepareAndSend(messageContent: ArrayList<String>, messageInfo: DexMessageInfo, metadata: DexMetadata, summary: SummaryInfo, eventHubSender: EventHubSender, eventHubName: String, context: ExecutionContext) {
        val contentBase64 = Base64.getEncoder().encodeToString(messageContent.joinToString("\n").toByteArray())
        val msgEvent = DexEventPayload(contentBase64, messageInfo, metadata, summary)
        context.logger.info("Sending new Event to event hub Message: --> messageUUID: ${msgEvent.messageUUID}, messageIndex: ${msgEvent.metadata.provenance.messageIndex}, fileName: ${msgEvent.metadata.provenance.filePath}")
        val jsonMessage = gson.toJson(msgEvent)
        eventHubSender.send(evHubTopicName=eventHubName, message=jsonMessage)
        context.logger.info("full message: $jsonMessage")
        context.logger.info("Processed and Sent to event hub $eventHubName Message: --> messageUUID: ${msgEvent.messageUUID}, messageIndex: ${msgEvent.metadata.provenance.messageIndex}, fileName: ${msgEvent.metadata.provenance.filePath}")
        //println(msgEvent)
    }

    private fun validateMessageMetaData(metaDataMap: Map<String, String>, context: ExecutionContext):Boolean {
        var isValid = true;
        //Check if required Meta data fields are present
        val messageType = metaDataMap["message_type"];
        val route = metaDataMap["route"];
        val reportingJurisdiction = metaDataMap["reporting_jurisdiction"];
        context.logger.info("MetaData Info: --> messageType: ${messageType}, route: ${route}, reportingJurisdiction: $reportingJurisdiction")

        if(messageType.isNullOrEmpty()){
            isValid = false;
        } else if (messageType == HL7MessageType.ELR.name){
            if(route.isNullOrEmpty() || reportingJurisdiction.isNullOrEmpty()){
                isValid = false;
            }
        }
        context.logger.info("isValid: --> $isValid")

        return isValid;
    }


} // .class  Function

