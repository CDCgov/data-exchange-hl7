package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventGridTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import gov.cdc.dex.metadata.*
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.StringUtils.Companion.hashMD5
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.IllegalArgumentException
import java.util.Date
import java.util.Base64.getEncoder
import kotlin.collections.ArrayList


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
            "data_stream_id", "meta_destination_id",
            "data_stream_route", "meta_ext_event",
            "data_producer_id",
            "jurisdiction", "reporting_jurisdiction", "meta_organization",
            "sender_id", "user_id", "meta_username",
            "upload_id", "meta_ext_uploadid", "tus_tguid",
            "trace_id",
            "parent_span_id"
        )

        val fnConfig = FunctionConfig()
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    }

    @FunctionName("ingest-file")
   /* fun processEventGrid(
        @EventGridTrigger(name = "eventGridEvent") messageEvent: String?,
        context: ExecutionContext
    ): DexHL7Metadata? {*/
    fun processQueue(
        @QueueTrigger(name = "message", queueName = "hl7-file-drop", connection = "BlobIngestConnectionString") message: String?,
        context: ExecutionContext
    ): DexHL7Metadata? {
        logger.info("DEX::Received BLOB_CREATED event!")
        var msgEvent: DexHL7Metadata? = null
        val eventReportList = mutableListOf<String>()
        context.logger.fine("payload ingest-file:$message")
        try {
            val event = gson.fromJson(message, AzBlobCreateEventMessage::class.java)
            val startTime = Date().toIsoString()
            // Pick up blob metadata
            val blobName = event.eventData.url.substringAfter("/${fnConfig.blobIngestContName}/")
            logger.info("DEX::Reading blob: $blobName")
            val blobClient = fnConfig.azBlobProxy.getBlobClient(blobName)
            // Create Map of Blob Metadata with lower case keys
            val metaDataMap = blobClient.properties.metadata.mapKeys { it.key.lowercase() }.toMutableMap()
            // filter out unknown metadata and store in dynamicMetadata
            val dynamicMetadata = metaDataMap.filter { e -> !knownMetadata.contains(e.key) }
            val sourceMetadata = dynamicMetadata.ifEmpty { null }
            // add other event/blob properties we need
            metaDataMap["file_path"] = event.eventData.url
            metaDataMap["file_timestamp"] = blobClient.properties.lastModified.toIsoString()
            metaDataMap["file_size"] = blobClient.properties.blobSize.toString()

            // Add routing data to Report object for this file
            val eventReport = ReceiverEventReport()
            val routingMetadata = buildRoutingMetadata(metaDataMap, sourceMetadata)
            eventReport.routingData = routingMetadata

            // Read Blob File by Lines
            // -------------------------------------
            val reader = InputStreamReader(blobClient.openInputStream(), Charsets.UTF_8)
            val currentLinesArr = arrayListOf<String>()
            var mshCount = 0
            var messageIndex = 1
            var singleOrBatch = MessageMetadata.SINGLE_FILE
            BufferedReader(reader).use { br ->
                br.forEachLine { line ->
                    val lineClean =
                        line.trim().let { if (it.startsWith(UTF_BOM)) it.substring(1) else it }
                    if (lineClean.startsWith("FHS") ||
                        lineClean.startsWith("BHS") ||
                        lineClean.startsWith("BTS") ||
                        lineClean.startsWith("FTS")
                    ) {
                        singleOrBatch = MessageMetadata.BATCH_FILE
                        // batch line --Nothing to do here
                    } else if (lineClean.isNotEmpty()) {
                        if (lineClean.startsWith("MSH")) {
                            mshCount++
                            if (mshCount > 1) {
                                singleOrBatch = MessageMetadata.BATCH_FILE
                                buildAndSendMessage(
                                    messageIndex = messageIndex,
                                    singleOrBatch = singleOrBatch,
                                    status = STATUS_SUCCESS,
                                    currentLinesArr = currentLinesArr,
                                    eventTime = event.eventTime,
                                    startTime = startTime,
                                    routingMetadata = routingMetadata,
                                    eventReport = eventReport
                                )
                                messageIndex++
                            }
                            currentLinesArr.clear()
                        } // .if
                        currentLinesArr.add(lineClean)
                    } // .else
                } // .forEachLine
            } // .BufferedReader

            msgEvent = if (mshCount > 0) {
                // Send last message
                buildAndSendMessage(
                    messageIndex = messageIndex,
                    singleOrBatch = singleOrBatch,
                    status = STATUS_SUCCESS,
                    currentLinesArr = currentLinesArr,
                    eventTime = event.eventTime,
                    startTime = startTime,
                    routingMetadata = routingMetadata,
                    eventReport = eventReport
                )
            } else {
                // no valid message -- send to error queue
                // send empty array as message content when content is invalid
                val errorMessage = "No valid message found."
                buildAndSendMessage(
                    messageIndex = messageIndex,
                    singleOrBatch = singleOrBatch,
                    status = STATUS_ERROR,
                    currentLinesArr = arrayListOf(),
                    eventTime = event.eventTime,
                    startTime = startTime,
                    routingMetadata = routingMetadata,
                    eventReport = eventReport,
                    errorMessage = errorMessage
                )

            }

            eventReport.messageBatch = singleOrBatch
            eventReport.totalMessageCount = messageIndex
            eventReportList.add(gson.toJson(eventReport))
            logger.info("file event report --> ${gson.toJson(eventReport)}")

        } catch (e: Exception) {
            logger.error("Failure in Recdeb fn: ${e.message}")
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

    private fun buildAndSendMessage(
        messageIndex: Int,
        singleOrBatch: String,
        status: String,
        currentLinesArr: ArrayList<String>,
        eventTime: String,
        startTime: String,
        routingMetadata: RoutingMetadata,
        eventReport: ReceiverEventReport,
        errorMessage: String? = null
    ): DexHL7Metadata {
        val messageMetadata = MessageMetadata(
            singleOrBatch = singleOrBatch,
            messageIndex = messageIndex,
            messageHash = currentLinesArr.joinToString("\n").hashMD5()
        )

        val (stage, summary) = buildStageAndSummaryMetadata(
            status = status,
            eventTimestamp = eventTime,
            startTime = startTime,
            errorMessage = errorMessage
        )
        return preparePayload(
            messageMetadata = messageMetadata,
            routingMetadata = routingMetadata,
            stage = stage,
            messageContent = currentLinesArr,
            summary = summary
        ).apply {
            //Send message to eh and update event report
            sendMessageAndUpdateEventReport(this, eventReport)
        }
    }

    private fun sendMessageAndUpdateEventReport(payload: DexHL7Metadata, eventReport: ReceiverEventReport) : DexHL7Metadata? {
        try {
            logger.info("DEX::Processed messageUUID: ${payload.messageMetadata.messageUUID}")
            val errors = fnConfig.evHubSenderOut.send(gson.toJson(payload))
            if (errors.isEmpty()) {
                logger.info("DEX::Sent messageUUID ${payload.messageMetadata.messageUUID} to ${fnConfig.evHubSendName}")
            } else {
                throw IllegalArgumentException("Message ${payload.messageMetadata.messageUUID} is too large for Event Hub")
            }

        } catch (e: Exception) {
            addErrorToReport(
                eventReport = eventReport,
                errorMessage = "Unable to send to event hub ${fnConfig.evHubSendName}: ${e.message}",
                messageUUID = payload.messageMetadata.messageUUID,
                messageIndex = payload.messageMetadata.messageIndex
            )
            eventReport.notPropogatedCount++
        }
        return payload
    }

    private fun addErrorToReport(
        eventReport: ReceiverEventReport,
        errorMessage: String,
        messageUUID: String? = null,
        messageIndex: Int = 1
    ) {
        eventReport.errorMessages.add(ReceiverEventError(messageIndex, messageUUID, errorMessage))
    }

    private fun buildStageAndSummaryMetadata(
        status: String,
        eventTimestamp: String,
        startTime: String,
        errorMessage: String? = null
    ): Pair<StageMetadata, SummaryInfo> {
        val stageMetadata = ReceiverStageMetadata(receiverStatus = status, eventTimestamp = eventTimestamp)
        stageMetadata.startProcessTime = startTime
        stageMetadata.endProcessTime = Date().toIsoString()
        var summary = SummaryInfo("RECEIVED")
        if (status == STATUS_ERROR) {
            summary = SummaryInfo("REJECTED")
            summary.problem =
                errorMessage?.let { Problem(processName = ReceiverStageMetadata.RECEIVER_PROCESS, errorMessage = it) }
        }
        return stageMetadata to summary
    }

    private fun getValueOrDefaultString(
        metaDataMap: Map<String, String?>,
        keysToTry: List<String>,
        defaultReturnValue: String = "UNKNOWN"
    ): String {
        keysToTry.forEach { if (!metaDataMap[it].isNullOrEmpty()) return metaDataMap[it]!! }
        return defaultReturnValue
    }

    private fun getValueOrNullString(
        metaDataMap: Map<String, String?>,
        keysToTry: List<String>
    ): String {
        val value = getValueOrDefaultString(metaDataMap, keysToTry)
        return if (value == "UNKNOWN") {
            ""
        } else {
            value
        }
    }

    private fun buildRoutingMetadata(
        metaDataMap: Map<String, String?>,
        supportingMetadata: Map<String, String>?
    ): RoutingMetadata {

        return RoutingMetadata(
            ingestedFilePath = metaDataMap["file_path"] ?: "",
            ingestedFileTimestamp = metaDataMap["file_timestamp"] ?: "",
            ingestedFileSize = metaDataMap["file_size"] ?: "",
            dataProducerId = metaDataMap["data_producer_id"]?:"",
            jurisdiction = getValueOrNullString(
                metaDataMap,
                listOf("jurisdiction", "reporting_jurisdiction", "meta_organization")
            ),
            uploadId = getValueOrDefaultString(metaDataMap, listOf("upload_id", "meta_ext_uploadid", "tus_tguid")),
            dataStreamId = getValueOrDefaultString(metaDataMap, listOf("data_stream_id", "meta_destination_id")),
            dataStreamRoute = getValueOrDefaultString(metaDataMap, listOf("data_stream_route", "meta_ext_event")),
            traceId = getValueOrDefaultString(metaDataMap, listOf("trace_id")),
            spanId = getValueOrDefaultString(metaDataMap, listOf( "parent_span_id", "span_id")),
            supportingMetadata = supportingMetadata
        )
    }


    private fun preparePayload(
        messageMetadata: MessageMetadata,
        routingMetadata: RoutingMetadata,
        stage: StageMetadata,
        messageContent: ArrayList<String>,
        summary: SummaryInfo,
    ): DexHL7Metadata {

        return DexHL7Metadata(
            messageMetadata = messageMetadata,
            routingMetadata = routingMetadata,
            stage = stage,
            content = getEncoder().encodeToString(messageContent.joinToString("\n").toByteArray()),
            summary = summary
        )
    }


} // .class  Function