import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.receiver.Function
import gov.cdc.dex.hl7.receiver.Function.Companion.UTF_BOM
import gov.cdc.dex.hl7.receiver.ReceiverProcessMetadata
import gov.cdc.dex.metadata.*
import gov.cdc.dex.mmg.InvalidConditionException
import gov.cdc.dex.mmg.MmgUtil
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.StringUtils.Companion.hashMD5
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

    @Test fun testDebatcherWithErrs() {
        println("Starting debatcher test")
        val redisName: String = System.getenv("REDIS_CACHE_NAME")
        val redisKey: String = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(redisName, redisKey)
        val mmgUtil = MmgUtil(redisProxy)
//FILENAME
        //val filePath = "other/GEN_SUMMARY_CASE_MAP_V1.0.json"  //invalid hl7
        //val filePath = "genV1/empty.txt"  // empty file
        //val filePath = "genV1/Genv1-Case-TestMessage2.HL7" //condition is not in redis
        // val filePath = "genV1/Genv1-Case-TestMessage1.HL7"  //condition is in redis (should succeed)
        val filePath = "genV1/phin-notification-message-specification-profile-v-2.0.pdf" //file too large and invalid hl7
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
                            val messageInfo = getMessageInfo(mmgUtil, currentLinesArr.joinToString("\n" ))
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
            val messageInfo = getMessageInfo(mmgUtil, currentLinesArr.joinToString("\n" ))
            val (metadata, summary) = buildMetadata(Function.STATUS_SUCCESS, eventHubMD, startTime, provenance)
            prepareAndSend(currentLinesArr, messageInfo, metadata, summary)
        } else {
            // no valid message -- send to error queue
            val (metadata, summary) = buildMetadata(Function.STATUS_ERROR, eventHubMD,startTime, provenance, "No valid message found.")
            prepareAndSend(arrayListOf(), DexMessageInfo(null, null, null, null, HL7MessageType.CASE), metadata, summary)
        }
    } // .test

    private fun getMessageInfo(mmgUtil: MmgUtil, message: String): DexMessageInfo {
        val msh21Gen = extractValue(message, Function.MSH_21_2_1_PATH)
        println("msh21Gen: $msh21Gen")
        val msh21Cond = extractValue(message, Function.MSH_21_3_1_PATH)
        println("msh21Cond: $msh21Cond")
        val eventCode = extractValue(message, Function.EVENT_CODE_PATH)
        println("eventCode: $eventCode")
        var jurisdictionCode = extractValue(message, Function.JURISDICTION_CODE_PATH)
        if (jurisdictionCode.isEmpty()) {
            jurisdictionCode = extractValue(message, Function.ALT_JURISDICTION_CODE_PATH)
        }
        println("jurisdictionCode: $jurisdictionCode")
        return try {
            val messageInfo = mmgUtil.getMMGMessageInfo(msh21Gen, msh21Cond, eventCode, jurisdictionCode)
            println("Try succeeded")
            messageInfo
        } catch (e : InvalidConditionException) {
            // TODO: SHOULD WE RECORD THIS ERROR MESSAGE?
            println("Try failed: ${e.message}")
            DexMessageInfo(eventCode, null, null, jurisdictionCode,HL7MessageType.CASE)
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

}