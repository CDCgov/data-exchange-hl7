package gov.cdc.dex.hl7

import com.azure.core.http.rest.Response
import com.azure.core.util.Context
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.models.BlobProperties
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.QueueTrigger
import gov.cdc.dex.util.ProcessingStatus.PSClientUtility
import gov.cdc.dex.metadata.*
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.StringUtils.Companion.hashMD5
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.Base64.getEncoder
import java.util.concurrent.TimeUnit


/**
 * Azure Functions with Event Hub Trigger.
 */
class Function {
    companion object {
        const val UTF_BOM = "\uFEFF"
        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_ERROR = "ERROR"
        const val UNKNOWN_VALUE = "UNKNOWN"
        val gson: Gson = GsonBuilder().serializeNulls().create()
        val knownMetadata: Set<String> = setOf(
            "data_stream_id", "meta_destination_id",
            "data_stream_route", "meta_ext_event",
            "data_producer_id",
            "jurisdiction", "reporting_jurisdiction", "meta_organization",
            "sender_id", "user_id", "meta_username",
            "upload_id", "tus_tguid",
            "trace_id",
            "parent_span_id", "span_id"
        )

        val fnConfig = FunctionConfig()
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    }

    @FunctionName("ingest-file")
    fun processQueue(
        @QueueTrigger(
            name = "message",
            queueName = "%queueName%",
            connection = "BlobIngestConnectionString"
        ) message: String?,
        context: ExecutionContext
    ): DexHL7Metadata? {
        logger.info("DEX::Received BLOB_CREATED event!")
        var msgMetadata: DexHL7Metadata? = null
        val eventReportList = mutableListOf<String>()
        context.logger.fine("payload ingest-file:$message")
        try {
            val event = gson.fromJson(message, AzBlobCreateEventMessage::class.java)
            val startTime = Date().toIsoString()
            // Initialize event report metadata
            val eventMetadata = ReceiverEventMetadata(
                stage =
                ReceiverEventStageMetadata(
                    startProcessingTime = startTime,
                    eventTimestamp = event.eventTime
                )
            )
            // create blob client
            val blobName = event.eventData.url.substringAfter("/${fnConfig.blobIngestContName}/")
            logger.info("DEX::Reading blob: $blobName")
            val blobClient = fnConfig.azBlobProxy.getBlobClient(blobName)

            // Get properties and metadata of blob -- should retry if failure.
            // if it cannot get properties after retrying, blobProperties will be null,
            // and we will log this blob as a failure (will fail validateMetadata)
            val blobProperties = getBlobProperties(blobClient)
            // Create Map of Blob Metadata with lower case keys
            val metaDataMap = blobProperties?.metadata?.mapKeys { it.key.lowercase() }?.toMutableMap() ?: mutableMapOf()
            // filter out unknown metadata and store in dynamicMetadata
            val dynamicMetadata = metaDataMap.filter { e -> !knownMetadata.contains(e.key) }
            val sourceMetadata = dynamicMetadata.ifEmpty { null }
            // add other event/blob properties we need
            metaDataMap["file_path"] = event.eventData.url
            metaDataMap["file_timestamp"] = blobProperties?.lastModified.toIsoString()
            metaDataMap["file_size"] = blobProperties?.blobSize.toString()

            // Add routing data and Report object for this file
            val eventReport = ReceiverEventReport()
            val routingMetadata = buildRoutingMetadata(metaDataMap, sourceMetadata)
            eventMetadata.routingData = routingMetadata
            var messageIndex = 1
            var singleOrBatch = MessageMetadata.SINGLE_FILE

            // error out if required metadata is missing
            if (validateMetadata(routingMetadata)) {
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

                msgMetadata = if (mshCount > 0) {
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
            } else {
                val errorMessage = "One or more required metadata elements are missing. " +
                        "data stream id is ${routingMetadata.dataStreamId}, upload id is ${routingMetadata.uploadId}"
                // if no upload_id, substitute blob name so file-sink does not overwrite "UNKNOWN.txt" record
                val newRoutingMetadata = if (routingMetadata.uploadId == UNKNOWN_VALUE) {
                    replaceUploadId(blobName, routingMetadata)
                } else {
                    routingMetadata
                }
                eventMetadata.routingData = newRoutingMetadata
                buildAndSendMessage(
                    messageIndex = messageIndex,
                    singleOrBatch = singleOrBatch,
                    status = STATUS_ERROR,
                    currentLinesArr = arrayListOf(),
                    eventTime = event.eventTime,
                    startTime = startTime,
                    routingMetadata = newRoutingMetadata,
                    eventReport = eventReport,
                    errorMessage = errorMessage
                )
            }
            // finalize event report and attach to event metadata
            eventReport.messageBatch = singleOrBatch
            eventReport.totalMessageCount = messageIndex
            eventMetadata.stage.endProcessingTime = Date().toIsoString()
            eventMetadata.stage.report = eventReport
            // add event metadata + report to list of reports to be sent later
            eventReportList.add(gson.toJson(eventMetadata))
            logger.debug("file event report --> ${gson.toJson(eventMetadata)}")

        } catch (e: Exception) {
            logger.error("Failure in Receiver-Debatcher function: ${e.message}")
        } finally {
            try {
                // send ingest-file event reports to separate event hub
                fnConfig.evHubSenderReports.send(eventReportList)
            } catch (e: Exception) {
                logger.error("Unable to send to event hub ${fnConfig.evReportsHubName}: ${e.message}")
            }
        }
        return msgMetadata
    }

    private fun validateMetadata(routingMetadata: RoutingMetadata): Boolean {
        return !(routingMetadata.dataStreamId == UNKNOWN_VALUE || routingMetadata.uploadId == UNKNOWN_VALUE)
    }

    private fun replaceUploadId(uploadId: String, currentMetadata: RoutingMetadata): RoutingMetadata {
        return RoutingMetadata(
            ingestedFilePath = currentMetadata.ingestedFilePath,
            ingestedFileTimestamp = currentMetadata.ingestedFileTimestamp,
            ingestedFileSize = currentMetadata.ingestedFileSize,
            dataProducerId = currentMetadata.dataProducerId,
            jurisdiction = currentMetadata.jurisdiction,
            uploadId = uploadId,
            dataStreamId = currentMetadata.dataStreamId,
            dataStreamRoute = currentMetadata.dataStreamRoute,
            traceId = currentMetadata.dataStreamId,
            spanId = currentMetadata.spanId,
            supportingMetadata = currentMetadata.supportingMetadata
        )
    }

    private fun getBlobProperties(blobClient: BlobClient): BlobProperties? {
        var blobProperties: BlobProperties? = null
        var response: Response<BlobProperties>
        var timeToWait = 0L
        var mustRetry: Boolean
        var retries = 3

        do {
            if (retries < 3) logger.info("RETRYING read of blob properties for blob ${blobClient.blobName}")
            try {
                response = blobClient.getPropertiesWithResponse(
                    null,
                    fnConfig.azBlobProxy.tryTimeout,
                    Context.NONE
                )
                mustRetry = (response.statusCode != 200 || response.value.metadata.isEmpty())
                if (mustRetry) {
                    logger.info("RETRY is TRUE: Response status code was ${response.statusCode}")
                } else {
                    blobProperties = response.value
                }
            } catch (e: Exception) {
                logger.info("ERROR in getBlobProperties: ${e.javaClass.canonicalName}: ${e.message}")
                mustRetry = true
            }
            retries--

            if (mustRetry) {
                try {
                    TimeUnit.SECONDS.sleep(timeToWait++)
                } catch (ex: InterruptedException) {
                    logger.debug("Timer interrupted")
                }
            }
        } while (mustRetry && retries > 0)

        return blobProperties
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

        val psClientUtility = PSClientUtility()
        logger.info("Span ID : ${ routingMetadata.spanId}")

        fnConfig.psURL?.let {
            psClientUtility.sendTraceToProcessingStatus(
                fnConfig.psURL,
                routingMetadata.traceId,
                routingMetadata.spanId,
                "startSpan"
            ).let {
                if(it.isNotEmpty()) routingMetadata.spanId = it
            }
            logger.info("Setting processing status spanId to routingMetadata.spanId: ${routingMetadata.spanId}")
        }

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

        if (!errorMessage.isNullOrEmpty()) {
            addErrorToReport(eventReport, errorMessage, messageMetadata.messageUUID, messageIndex)
            eventReport.notPropogatedCount++
        }


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

    private fun sendMessageAndUpdateEventReport(
        payload: DexHL7Metadata,
        eventReport: ReceiverEventReport
    ): DexHL7Metadata {
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
            // avoid adding to notPropagatedCount if the message has already been put in error status
            if (eventReport.errorMessages.count { it.messageUUID == payload.messageMetadata.messageUUID } == 1)
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
        val stageMetadata = ReceiverMessageStageMetadata(receiverStatus = status, eventTimestamp = eventTimestamp)
        stageMetadata.startProcessTime = startTime
        stageMetadata.endProcessTime = Date().toIsoString()
        var summary = SummaryInfo("RECEIVED")
        if (status == STATUS_ERROR) {
            summary = SummaryInfo("REJECTED")
            summary.problem =
                errorMessage?.let { Problem(processName = ProcessInfo.RECEIVER_PROCESS, errorMessage = it) }
        }
        return stageMetadata to summary
    }

    private fun getValueOrDefaultString(
        metaDataMap: Map<String, String?>,
        keysToTry: List<String>,
        defaultReturnValue: String = UNKNOWN_VALUE
    ): String {
        keysToTry.forEach { if (!metaDataMap[it].isNullOrEmpty()) return metaDataMap[it]!! }
        return defaultReturnValue
    }

    private fun buildRoutingMetadata(
        metaDataMap: Map<String, String?>,
        supportingMetadata: Map<String, String>?
    ): RoutingMetadata {

        return RoutingMetadata(
            ingestedFilePath = metaDataMap["file_path"] ?: "",
            ingestedFileTimestamp = metaDataMap["file_timestamp"] ?: "",
            ingestedFileSize = metaDataMap["file_size"] ?: "",
            dataProducerId = metaDataMap["data_producer_id"] ?: "",
            jurisdiction = getValueOrDefaultString(
                metaDataMap,
                listOf("jurisdiction", "reporting_jurisdiction", "meta_organization")
            ),
            uploadId = getValueOrDefaultString(metaDataMap, listOf("upload_id", "tus_tguid")),
            dataStreamId = getValueOrDefaultString(metaDataMap, listOf("data_stream_id", "meta_destination_id")),
            dataStreamRoute = getValueOrDefaultString(metaDataMap, listOf("data_stream_route", "meta_ext_event")),
            traceId = getValueOrDefaultString(metaDataMap, listOf("trace_id")),
            spanId = getValueOrDefaultString(metaDataMap, listOf("parent_span_id", "span_id")),
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