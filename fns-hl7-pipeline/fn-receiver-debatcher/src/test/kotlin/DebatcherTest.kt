import gov.cdc.dex.hl7.receiver.Function
import gov.cdc.dex.hl7.receiver.Function.Companion.UTF_BOM
import gov.cdc.dex.hl7.receiver.ReceiverProcessMetadata
import gov.cdc.dex.metadata.DexMetadata
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.Provenance
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.StringUtils.Companion.hashMD5

import org.junit.jupiter.api.Test

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
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

//    @Test
//    fun testDebatching() {
//        val filePath = "/genV1/Genv1-Case-TestMessage1.HL7"
//        val startTime = Date().toIsoString()
//        val testFileIS = this::class.java.getResource(filePath).openStream()
////        val testFileIS = this::class.java.getResource("/genV1/Genv1-Case-TestMessage1.HL7").openStream()
//        val provenance = Provenance(
//            eventId="123",
//            eventTimestamp = startTime,
//            filePath=filePath,
//            fileTimestamp=startTime,
//            fileSize=1234,
//            singleOrBatch= Provenance.SINGLE_FILE,
//            originalFileName ="blobName",
//            systemProvider = "BLOB"
//        )
//        val currentLinesArr = arrayListOf<String>()
//        var mshCount = 0
//        BufferedReader(InputStreamReader(testFileIS)).use { br ->
//            br.forEachLine {line ->
//                val lineClean = line.trim().let { if ( it.startsWith(UTF_BOM) )  it.substring(1)  else it}
//                if ( lineClean.startsWith("FHS") || lineClean.startsWith("BHS") || lineClean.startsWith("BTS") ) {
//                    // batch line --Nothing to do here
//                    provenance.singleOrBatch = Provenance.BATCH_FILE
//                } else {
//                    if ( lineClean.startsWith("MSH") ) {
//                        mshCount++
//                        if (mshCount > 1 ) {
//                            provenance.singleOrBatch = Provenance.BATCH_FILE
//                            println("Sending $provenance")
//                            println("Message: ${currentLinesArr.joinToString("\n")}")
//                            provenance.messageIndex++
//                        }
//
//                        currentLinesArr.clear()
//                    } // .if
//                    currentLinesArr.add(lineClean)
//                } // .else
//            }
//            if (mshCount > 0) {
//                println("Sending last one: $provenance")
//            } else {
//                println("Message invalid - no MSH found")
//            }
//            println("Last Message: ${currentLinesArr.joinToString("\n")}")
//        }
//    }

    @Test fun testDebatcherWithErrs() {
        val filePath = "/genV1/empty.txt"
        val startTime = Date().toIsoString()
        val testFileIS = this::class.java.getResource(filePath).openStream()
//        val testFileIS = this::class.java.getResource("/genV1/empty.txt).openStream()
        val provenance = Provenance(
            eventId="123",
            eventTimestamp = startTime,
            filePath=filePath,
            fileTimestamp=startTime,
            fileSize=1234,
            singleOrBatch= Provenance.SINGLE_FILE,
            originalFileName ="blobName",
            systemProvider = "BLOB"
        )
        // Read Blob File by Lines
        // -------------------------------------
        val reader = InputStreamReader(testFileIS)
        val currentLinesArr = arrayListOf<String>()
        var mshCount = 0
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
                            val (metadata, summary) = buildMetadata(Function.STATUS_SUCCESS, startTime, provenance)
                           // prepareAndSend(currentLinesArr, metadata, summary, evHubSender, evHubName, context)
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
            val (metadata, summary) = buildMetadata(Function.STATUS_SUCCESS, startTime, provenance)
            println("status: ${metadata.processes[0].status}, provenance: ${metadata.provenance.toString()}")
        } else {
            // no valid message -- send to error queue
            val (metadata, summary) = buildMetadata(Function.STATUS_ERROR, startTime, provenance, "No valid message found.")
            println("status: ${metadata.processes[0].status}, provenance: ${metadata.provenance.toString()}")
        }
    } // .test



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

//    @Test
//    fun testRemoveBomFromBlob() {
//        val blobContainerClient = BlobServiceClientBuilder()
//            .connectionString(System.getEnv("BLOB_STORAGE"))
//            .buildClient()
//            .getBlobContainerClient("hl7ingress")
//
//        val blobClient = blobContainerClient.getBlobClient("Genv2_2-0-1_TC08.txt")
//        val reader = InputStreamReader( blobClient.openInputStream(), Charsets.UTF_8 )
//        BufferedReader( reader ).use { br ->
//            br.forEachLine { line ->
//                var lineClean = line.trim()
//                while (lineClean.startsWith(UTF_BOM)) {
//                    //if (lineClean.startsWith(UTF_BOM)) {
//                    println("Found BOM...")
//                    lineClean = lineClean.substring(1)
//                    println(line.length)
//                    println(lineClean.length)
//                }
//            }
//        }
//    }
}