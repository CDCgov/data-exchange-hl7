import gov.cdc.dex.hl7.*
import gov.cdc.dex.hl7.Function
import gov.cdc.dex.hl7.Function.Companion.UTF_BOM
import gov.cdc.dex.metadata.*
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.StringUtils.Companion.hashMD5
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

class DebatcherTest {


    @Test
    fun testRemoveBom() {
        //val testFile = this::class.java.getResource("/Genv2_2-0-1_TC08.txt")
        val reader = File("src/test/resources/Genv2_2-0-1_TC08.txt").bufferedReader()
        BufferedReader(reader).use { br ->
            br.forEachLine { line ->
                //while ( br.readLine().also { line = it } != null) {
                //println(line)
                var lineClean = line.trim()
                while (lineClean.startsWith(UTF_BOM)) {
                    //if (lineClean.startsWith(UTF_BOM)) {
                    println("Found BOM...")
                    lineClean = lineClean.substring(1)
                    println(line.length)
                    println(lineClean.length)
                }
            }
        }
    }

    @Test
    fun testDebatcher() {
        println("Starting debatcher test")
        val filePath = "genV1/Genv1-Case-TestMessage1.HL7"
        val startTime = Date().toIsoString()
        val metaDataMap: Map<String, String?> = mapOf(
            Pair("reporting_jurisdiction", "16"),
            Pair("original_file_name", "Genv1-Case-TestMessage1.HL7"),
            Pair("meta_destination_id", "NNDSS"),
            Pair("meta_ext_event", "hl7"),
            Pair("tus_tguid", ""),
            Pair("trace_id", "unknown"),
            Pair("parent_span_id", "unknown"),
            Pair("file_path", filePath)
        )
        var msgEvent: DexHL7Metadata? = null
        val eventReportList = mutableListOf<String>()
        try {
            // initialize event report metadata
            val eventMetadata = ReceiverEventMetadata(stage =
                    ReceiverEventStageMetadata(startProcessingTime = startTime,
                         eventTimestamp = Date().toIsoString()))
            val testFileIS = this::class.java.getResource(filePath).openStream()
            // Add routing data to Report object for this file
            val eventReport = ReceiverEventReport()
            val routingMetadata = buildRoutingMetadata(metaDataMap, null)
            eventMetadata.routingData = routingMetadata
            var messageIndex = 1
            var singleOrBatch = MessageMetadata.SINGLE_FILE

            if (validateMetadata(routingMetadata)) {
                // Read Blob File by Lines
                // -------------------------------------
                val reader = InputStreamReader(testFileIS, Charsets.UTF_8)
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
                                        status = Function.STATUS_SUCCESS,
                                        currentLinesArr = currentLinesArr,
                                        eventTime = startTime,
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
                        status = Function.STATUS_SUCCESS,
                        currentLinesArr = currentLinesArr,
                        eventTime = startTime,
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
                        status = Function.STATUS_ERROR,
                        currentLinesArr = arrayListOf(),
                        eventTime = startTime,
                        startTime = startTime,
                        routingMetadata = routingMetadata,
                        eventReport = eventReport,
                        errorMessage = errorMessage
                    )

                }
            } else {
                val errorMessage = "Message is missing required metadata. " +
                        "data stream id is ${routingMetadata.dataStreamId}, upload id is ${routingMetadata.uploadId}, " +
                        "data stream route is ${routingMetadata.dataStreamRoute}"

                buildAndSendMessage(
                    messageIndex = messageIndex,
                    singleOrBatch = singleOrBatch,
                    status = Function.STATUS_ERROR,
                    currentLinesArr = arrayListOf(),
                    eventTime = startTime,
                    startTime = startTime,
                    routingMetadata = routingMetadata,
                    eventReport = eventReport,
                    errorMessage = errorMessage
                )

            }
            // finalize event report
            eventReport.messageBatch = singleOrBatch
            eventReport.totalMessageCount = messageIndex
            eventMetadata.stage.endProcessingTime = Date().toIsoString()
            eventMetadata.stage.report = eventReport
            eventReportList.add(Function.gson.toJson(eventMetadata))
            println("file event report --> ${Function.gson.toJson(eventMetadata)}")

        } catch (e: Exception) {
            println("Failure in Debatcher Test: ${e.message}")
        } finally {
            // send ingest-file event reports to separate event hub
            println("Simulating sending event report")

        }


    } // .test


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

        if (!errorMessage.isNullOrEmpty()) {
            addErrorToReport(eventReport,errorMessage,messageMetadata.messageUUID,messageIndex)
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

    private fun sendMessageAndUpdateEventReport(payload: DexHL7Metadata, eventReport: ReceiverEventReport) {
        val jsonMessage = Function.gson.toJson(payload)
        println(jsonMessage)
        println("Simulating Sending new Event to event hub Message: --> messageUUID: ${payload.messageMetadata.messageUUID}, messageIndex: ${payload.messageMetadata.messageIndex}, fileName: ${payload.routingMetadata.ingestedFilePath}")
        println("Processed and Sent to console Message: --> messageUUID: ${payload.messageMetadata.messageUUID}, messageIndex: ${payload.messageMetadata.messageIndex}, fileName: ${payload.routingMetadata.ingestedFilePath}")
    }
    private fun validateMetadata(routingMetadata: RoutingMetadata) : Boolean {
        return !(routingMetadata.dataStreamId == Function.UNKNOWN_VALUE || routingMetadata.uploadId == Function.UNKNOWN_VALUE)
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
        if (status == Function.STATUS_ERROR) {
            summary = SummaryInfo("REJECTED")
            summary.problem =
                errorMessage?.let { Problem(processName = ProcessInfo.RECEIVER_PROCESS, errorMessage = it) }
        }
        return stageMetadata to summary
    }

    private fun getValueOrDefaultString(
        metaDataMap: Map<String, String?>,
        keysToTry: List<String>,
        defaultReturnValue: String = Function.UNKNOWN_VALUE
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
            dataProducerId = metaDataMap["data_producer_id"]?:"",
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
            content = Base64.getEncoder().encodeToString(messageContent.joinToString("\n").toByteArray()),
            summary = summary
        )
    }

}