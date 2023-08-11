import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.Function
import gov.cdc.dex.hl7.Function.Companion.UTF_BOM
import gov.cdc.dex.hl7.ReceiverProcessMetadata
import gov.cdc.dex.metadata.*
import gov.cdc.dex.mmg.InvalidConditionException
import gov.cdc.dex.mmg.MmgUtil
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.StringUtils.Companion.hashMD5
import gov.cdc.dex.util.StringUtils.Companion.normalize
import gov.cdc.hl7.HL7StaticParser
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
        BufferedReader( reader ).use { br ->
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

    @Test fun testDebatcher() {
        println("Starting debatcher test")
        val filePath = "genV1/Genv1-Case-TestMessage1.HL7"
        val startTime = Date().toIsoString()
        val testFileIS = this::class.java.getResource(filePath).openStream()
        val provenance = Provenance(
            eventId="123",
            eventTimestamp = startTime,
            filePath=filePath,
            fileTimestamp=startTime,
            fileSize=1234,
            singleOrBatch= Provenance.SINGLE_FILE,
            originalFileName ="blobName",
            systemProvider = "BLOB",
            originalFileTimestamp=startTime,
        )
        // Read Blob File by Lines
        // -------------------------------------
        val reader = InputStreamReader(testFileIS)
        val currentLinesArr = arrayListOf<String>()
        var mshCount = 0
        val eventHubMD = EventHubMetadata(1, 1, null, "20230101")
        val metaDataMap =  mapOf<String, String>(Pair("route", ""))
        BufferedReader(reader).use { br ->
            br.forEachLine { line ->
                val lineClean = line.trim().let { if ( it.startsWith(UTF_BOM) )  it.substring(1)  else it}
                if ( lineClean.startsWith("FHS") || lineClean.startsWith("BHS") || lineClean.startsWith("BTS") || lineClean.startsWith("FTS") ) {
                    // batch line --Nothing to do here
                    provenance.singleOrBatch = Provenance.BATCH_FILE
                } else {
                    if ( lineClean.startsWith("MSH") ) {
                        mshCount++
                        if ( mshCount > 1 ) {
                            provenance.singleOrBatch = Provenance.BATCH_FILE
                            provenance.messageHash = currentLinesArr.joinToString("\n").hashMD5()
                            val messageInfo = getMessageInfo(metaDataMap, currentLinesArr.joinToString("\n"))
                            val (metadata, summary) = buildMetadata(Function.STATUS_SUCCESS, eventHubMD, startTime, provenance)
                            prepareAndSend(currentLinesArr, messageInfo, metadata, summary)
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
            val messageInfo = getMessageInfo(metaDataMap, currentLinesArr.joinToString("\n"))
            val (metadata, summary) = buildMetadata(Function.STATUS_SUCCESS, eventHubMD, startTime, provenance)
            prepareAndSend(currentLinesArr, messageInfo, metadata, summary)
        } else {
            // no valid message -- send to error queue
            val (metadata, summary) = buildMetadata(Function.STATUS_ERROR, eventHubMD,startTime, provenance, "No valid message found.")
            prepareAndSend(arrayListOf(), DexMessageInfo(null, null, null, null, HL7MessageType.CASE), metadata, summary)
        }
    } // .test


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
        if (status == Function.STATUS_ERROR) {
            summary = SummaryInfo("REJECTED")
            summary.problem= Problem(ReceiverProcessMetadata.RECEIVER_PROCESS, null, null, errorMessage, false, 0, 0)
        }
        return DexMetadata(provenance, listOf(processMD)) to summary
    }

    private fun prepareAndSend(messageContent: ArrayList<String>, messageInfo: DexMessageInfo, metadata: DexMetadata, summary: SummaryInfo) {
        val contentBase64 = Base64.getEncoder().encodeToString(messageContent.joinToString("\n").toByteArray())
        val msgEvent = DexEventPayload(contentBase64, messageInfo, metadata, summary)
        val jsonMessage = Function.gson.toJson(msgEvent)
        println(jsonMessage)
        println("Simulating Sending new Event to event hub Message: --> messageUUID: ${msgEvent.messageUUID}, messageIndex: ${msgEvent.metadata.provenance.messageIndex}, fileName: ${msgEvent.metadata.provenance.filePath}")
       // eventHubSender.send(evHubTopicName=eventHubName, message=jsonMessage)
        println("Processed and Sent to console Message: --> messageUUID: ${msgEvent.messageUUID}, messageIndex: ${msgEvent.metadata.provenance.messageIndex}, fileName: ${msgEvent.metadata.provenance.filePath}")
    }
    private fun getMessageInfo(metaDataMap: Map<String, String>, message: String): DexMessageInfo {
        val eventCode = extractValue(message, Function.EVENT_CODE_PATH)
        val localRecordID = extractValue(message, Function.LOCAL_RECORD_ID_PATH)
        val messageType = metaDataMap["message_type"]

        //READ FROM METADATA FOR ELR
        if (messageType == HL7MessageType.ELR.name) {
            val route = metaDataMap["route"]?.normalize()
            val reportingJurisdiction = metaDataMap["reporting_jurisdiction"]
            return DexMessageInfo(eventCode, route, null, reportingJurisdiction, HL7MessageType.ELR, localRecordID)
        }

        var jurisdictionCode = extractValue(message, Function.JURISDICTION_CODE_PATH)
        if (jurisdictionCode.isEmpty()) {
            jurisdictionCode = extractValue(message, Function.ALT_JURISDICTION_CODE_PATH)
        }

        return DexMessageInfo(eventCode = eventCode,
            route = Function.fnConfig.eventCodes[eventCode]?.get("category"),
            mmgKeyList = null,
            jurisdictionCode =  jurisdictionCode,
            type =  HL7MessageType.CASE,
            localRecordID = localRecordID)
    }

}