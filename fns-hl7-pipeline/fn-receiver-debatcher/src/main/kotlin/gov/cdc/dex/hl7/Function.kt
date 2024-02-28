package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import gov.cdc.dex.metadata.*
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.StringUtils.Companion.hashMD5
import gov.cdc.dex.util.StringUtils.Companion.normalize
import gov.cdc.hl7.HL7StaticParser
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.util.*
import java.util.Base64.getEncoder


/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    companion object {
        const val UTF_BOM = "\uFEFF"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_ERROR = "ERROR"
        const val EVENT_CODE_PATH = "OBR-31.1"
        const val JURISDICTION_CODE_PATH = "OBX[@3.1='77968-6']-5.1"
        const val ALT_JURISDICTION_CODE_PATH = "OBX[@3.1='NOT116']-5.1"
        const val LOCAL_RECORD_ID_PATH = "OBR-3.1"
        val gson: Gson = GsonBuilder().serializeNulls().create()
        val knownMetadata: Set<String> = setOf(
            "message_type",
            "route",
            "reporting_jurisdiction",
            "original_file_name",
            "original_file_timestamp",
            "system_provider",
            "meta_destination_id",
            "meta_ext_event",
            "tus_tguid",
            "meta_ext_uploadid",
            "trace_id",
            "parent_span_id"
        )

        val fnConfig = FunctionConfig()
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    }

    private fun sendMessageAndUpdateEventReport(payload: DexEventPayload, eventReport: ReceiverEventReport) {
        try {
            logger.info("DEX::Processed messageUUID: ${payload.messageUUID}")
            val errors = fnConfig.evHubSenderOut.send(gson.toJson(payload))
            if (errors.isEmpty()) {
                logger.info("DEX::Sent messageUUID ${payload.messageUUID} to ${fnConfig.evHubSendName}")
            } else {
                throw IllegalArgumentException("Message ${payload.messageUUID} is too large for Event Hub")
            }

        } catch (e: Exception) {
            addErrorToReport(
                eventReport = eventReport,
                errorMessage = "Unable to send to event hub ${fnConfig.evHubSendName}: ${e.message}",
                messageUUID = payload.messageUUID,
                messageIndex = payload.metadata.provenance.messageIndex
            )
            eventReport.notPropogatedCount++
        }
    }

    private fun addErrorToReport(
        eventReport: ReceiverEventReport,
        errorMessage: String,
        messageUUID: String? = null,
        messageIndex: Int = 1
    ) {
        eventReport.errorMessages.add(ReceiverEventError(messageIndex, messageUUID, errorMessage))
    }

    private fun getMessageInfo(metaDataMap: Map<String, String?>, message: String): DexMessageInfo {
        val eventCode = extractValue(message, EVENT_CODE_PATH)
        val localRecordID = extractValue(message, LOCAL_RECORD_ID_PATH)
        val messageType = metaDataMap["message_type"]

        //READ FROM METADATA FOR ELR
        if (messageType == HL7MessageType.ELR.name) {
            val route = metaDataMap["route"]?.normalize()
            val reportingJurisdiction = metaDataMap["reporting_jurisdiction"]
            return DexMessageInfo(eventCode, route, null, reportingJurisdiction, HL7MessageType.ELR, localRecordID)
        }

        var jurisdictionCode = extractValue(message, JURISDICTION_CODE_PATH)
        if (jurisdictionCode.isEmpty()) {
            jurisdictionCode = extractValue(message, ALT_JURISDICTION_CODE_PATH)
        }

        return DexMessageInfo(
            eventCode = eventCode,
            route = fnConfig.eventCodes[eventCode]?.get("category") ?: "",
            mmgKeyList = null,
            jurisdictionCode = jurisdictionCode,
            type = HL7MessageType.CASE,
            localRecordID = localRecordID
        )
    }

    private fun extractValue(msg: String, path: String): String {
        val value = HL7StaticParser.getFirstValue(msg, path)
        return if (value.isDefined) value.get()
        else ""
    }

    private fun buildMetadata(
        status: String,
        eventTimestamp: String,
        startTime: String,
        provenance: Provenance,
        errorMessage: String? = null
    ): Pair<DexMetadata, SummaryInfo> {
        val processMD = ReceiverProcessMetadata(status, eventTimestamp)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()
        var summary = SummaryInfo("RECEIVED")
        if (status == STATUS_ERROR) {
            summary = SummaryInfo("REJECTED")
            summary.problem = Problem(ReceiverProcessMetadata.RECEIVER_PROCESS, null, null, errorMessage, false, 0, 0)
        }
        return DexMetadata(provenance, processMD) to summary
    }


    private fun getRoutingData(metaDataMap: Map<String, String?>): RoutingMetadata {
        val uploadID = if (!metaDataMap["meta_ext_uploadid"].isNullOrEmpty()) {
            metaDataMap["meta_ext_uploadid"]
        } else if (!metaDataMap["tus_tguid"].isNullOrEmpty()) {
            metaDataMap["tus_tguid"]
        } else "UNKNOWN"
        val traceID = metaDataMap["trace_id"]
        val parentSpanID = metaDataMap["parent_span_id"]

        return RoutingMetadata(
            uploadID = uploadID,
            traceID = traceID,
            parentSpanID = parentSpanID,
            destinationID = metaDataMap["meta_destination_id"],
            destinationEvent = metaDataMap["meta_ext_event"]
        )
    }

    private fun preparePayload(
        messageContent: ArrayList<String>,
        messageInfo: DexMessageInfo,
        metadata: DexMetadata,
        summary: SummaryInfo,
        metaDataMap: Map<String, String?>
    ): DexEventPayload {

        return DexEventPayload(
            messageInfo = messageInfo,
            metadata = metadata,
            summary = summary,
            routingMetadata = getRoutingData(metaDataMap),
            content = getEncoder().encodeToString(messageContent.joinToString("\n").toByteArray())
        )
    }


    private fun validateMessageMetadata(metaDataMap: Map<String, String?>): Pair<Boolean, String> {
        var isValid = true
        //Check if required Metadata fields are present
        val messageType = if (metaDataMap.containsKey("message_type")) {
            metaDataMap["message_type"]?.uppercase() ?: HL7MessageType.UNKNOWN.name
        } else {
            "UNKNOWN"
        }
        val route = metaDataMap["route"]
        val reportingJurisdiction = metaDataMap["reporting_jurisdiction"]

        if (messageType.isEmpty()) {
            isValid = false
        } else if (messageType == HL7MessageType.ELR.name) {
            if (route.isNullOrEmpty() || reportingJurisdiction.isNullOrEmpty()) {
                isValid = false
            }
        }
        logger.info("DEX::Metadata Info: --> isValid: $isValid;  messageType: ${messageType}, route: ${route}, reportingJurisdiction: $reportingJurisdiction")
        return Pair(isValid, messageType)
    }

    @FunctionName("ingest-file")
    /*fun processEventGrid(
        @EventGridTrigger(name = "eventGridEvent") messageEvent: String?,
        context: ExecutionContext
    ): DexEventPayload? {*/
    fun processQueue(
        @QueueTrigger(name = "message", queueName = "hl7-file-drop", connection = "BlobIngestConnectionString") message: String?,
        context: ExecutionContext
    ): DexEventPayload? {
        logger.info("DEX::Received BLOB_CREATED event!")
        var msgEvent: DexEventPayload? = null
        val eventReportList = mutableListOf<String>()
        context.logger.fine("payload recdeb002:$message")
        try {
            val event = gson.fromJson(message, AzBlobCreateEventMessage::class.java)
            val startTime = Date().toIsoString()
            // Pick up blob metadata
            val blobName = event.eventData.url.substringAfter("/${fnConfig.blobIngestContName}/")

            // Create initial Report object for this file
            val eventReport = ReceiverEventReport(fileName = blobName)
            // Create blob reader
            logger.info("DEX::Reading blob: $blobName")
            val blobClient = fnConfig.azBlobProxy.getBlobClient(blobName)
            // Create Map of Blob Metadata with lower case keys
            val metaDataMap = blobClient.properties.metadata.mapKeys { it.key.lowercase() }
            // filter out known/required metadata and store the rest in
            val dynamicMetadata = metaDataMap.filter { e -> !knownMetadata.contains(e.key)}

            // Create Metadata for Provenance
            val provenance = Provenance(
                eventId = event.id,
                eventTimestamp = event.eventTime,
                filePath = event.eventData.url,
                fileTimestamp = blobClient.properties.lastModified.toIsoString(),
                fileSize = blobClient.properties.blobSize,
                singleOrBatch = Provenance.SINGLE_FILE,
                originalFileName = metaDataMap["original_file_name"] ?: blobName,
                systemProvider = metaDataMap["system_provider"],
                originalFileTimestamp = metaDataMap["original_file_timestamp"],
                sourceMetadata = dynamicMetadata.ifEmpty { null }
            ) // .hl7MessageMetadata

            // Add data to Report object for this file
            eventReport.routingData = getRoutingData(metaDataMap)
            eventReport.fileID = provenance.fileUUID

            //Validate metadata
            val validationResult = validateMessageMetadata(metaDataMap)
            val isValidMessage = validationResult.first
            val messageType = validationResult.second


            if (!isValidMessage) {
                // required Metadata is missing -- send to error queue
                val errorMessage = "Message missing required metadata."
                val (metadata, summary) = buildMetadata(
                    STATUS_ERROR,
                    event.eventTime,
                    startTime,
                    provenance,
                    errorMessage
                )

                // send empty array as message content when content is invalid
                msgEvent = preparePayload(
                    arrayListOf(),
                    DexMessageInfo(null, null, null, null, HL7MessageType.valueOf(messageType)),
                    metadata,
                    summary,
                    metaDataMap
                ).apply {
                    addErrorToReport(eventReport, errorMessage, this.messageUUID)
                    sendMessageAndUpdateEventReport(this, eventReport)
                }

            } else {
                // Read Blob File by Lines
                // -------------------------------------
                val reader = InputStreamReader(blobClient.openInputStream(), Charsets.UTF_8)
                val currentLinesArr = arrayListOf<String>()
                var mshCount = 0
                BufferedReader(reader).use { br ->
                    br.forEachLine { line ->
                        val lineClean =
                            line.trim().let { if (it.startsWith(UTF_BOM)) it.substring(1) else it }
                        if (lineClean.startsWith("FHS") ||
                            lineClean.startsWith("BHS") ||
                            lineClean.startsWith("BTS") ||
                            lineClean.startsWith("FTS")
                        ) {
                            // batch line --Nothing to do here
                            provenance.singleOrBatch = Provenance.BATCH_FILE
                        } else if (lineClean.isNotEmpty()) {
                            if (lineClean.startsWith("MSH")) {
                                mshCount++
                                if (mshCount > 1) {
                                    provenance.singleOrBatch = Provenance.BATCH_FILE
                                    provenance.messageHash = currentLinesArr.joinToString("\n").hashMD5()
                                    val messageInfo =
                                        getMessageInfo(metaDataMap, currentLinesArr.joinToString("\n"))


                                    val (metadata, summary) = buildMetadata(
                                        STATUS_SUCCESS,
                                        event.eventTime,
                                        startTime,
                                        provenance
                                    )
                                    msgEvent = preparePayload(
                                        currentLinesArr, messageInfo, metadata, summary, metaDataMap
                                    ).apply {
                                        //Send message to eh and update event report
                                        sendMessageAndUpdateEventReport(this, eventReport)
                                    }
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

                msgEvent = if (mshCount > 0) {
                    val (metadata, summary) = buildMetadata(
                        STATUS_SUCCESS,
                        event.eventTime,
                        startTime,
                        provenance
                    )
                    val messageInfo = getMessageInfo(metaDataMap, currentLinesArr.joinToString("\n"))
                    logger.info("message info --> ${gson.toJson(messageInfo)}")
                    preparePayload(
                        currentLinesArr, messageInfo, metadata, summary, metaDataMap
                    ).apply {
                        //send message to event hub and update event report
                        sendMessageAndUpdateEventReport(this, eventReport)
                    }
                } else {
                    // no valid message -- send to error queue
                    val errorMessage = "No valid message found."
                    val (metadata, summary) = buildMetadata(
                        STATUS_ERROR,
                        event.eventTime,
                        startTime,
                        provenance,
                        errorMessage
                    )
                    // send empty array as message content when content is invalid
                    preparePayload(
                        arrayListOf(), DexMessageInfo(
                            null, null, null, null,
                            HL7MessageType.valueOf(messageType)
                        ),
                        metadata, summary, metaDataMap
                    ).apply {
                        addErrorToReport(
                            eventReport,
                            errorMessage,
                            this.messageUUID,
                            provenance.messageIndex
                        )
                        //send message to eh and update event report
                        sendMessageAndUpdateEventReport(this, eventReport)
                    }
                }
            }

            eventReport.messageBatch = provenance.singleOrBatch
            eventReport.totalMessageCount = provenance.messageIndex
            eventReportList.add(gson.toJson(eventReport))
            logger.info("file event report --> ${gson.toJson(eventReport)}")

        } catch (e: Exception) {
            logger.error("Failure in Recdeb fn: ${e.message}")
            //throw Exception("Failure in Recdeb function ::${e.message}")
        } finally {
            try {
                // send ingest-file event reports to separate event hub
                fnConfig.evHubSenderReports.send(eventReportList)
            } catch (e: Exception) {
                logger.error("Unable to send to event hub ${fnConfig.evReportsHubName}: ${e.message}")
            }
        }
         return msgEvent
    }
} // .class  Function