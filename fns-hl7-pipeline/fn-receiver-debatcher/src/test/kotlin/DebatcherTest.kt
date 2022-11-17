import gov.cdc.dex.hl7.receiver.Function.Companion.UTF_BOM
import gov.cdc.dex.metadata.Provenance
import gov.cdc.dex.util.DateHelper.toIsoString

import org.junit.jupiter.api.Test

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

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

    @Test
    fun testDebatching() {

        val testFileIS = this::class.java.getResource("/genV1/GenV1_Batch_no_headers.txt").openStream()
//        val testFileIS = this::class.java.getResource("/genV1/Genv1-Case-TestMessage1.HL7").openStream()
        val provenance = Provenance(
            eventId="123",
            eventTimestamp = "2022-11-17",
            filePath="src/test/genV1/GenV1_Batch_no_headers",
            fileTimestamp="2022-11-17",
            fileSize=1234,
            singleOrBatch= Provenance.SINGLE_FILE,
            originalFileName ="blobName",
            systemProvider = "BLOB"
        )
        val currentLinesArr = arrayListOf<String>()
        var mshCount = 0
        BufferedReader(InputStreamReader(testFileIS)).use { br ->
            br.forEachLine {line ->
                val lineClean = line.trim().let { if ( it.startsWith(UTF_BOM) )  it.substring(1)  else it}
                if ( lineClean.startsWith("FHS") || lineClean.startsWith("BHS") || lineClean.startsWith("BTS") || lineClean.startsWith("BHS")  ) {
                    // batch line --Nothing to do here
                    provenance.singleOrBatch = Provenance.BATCH_FILE
                } else {
                    if ( lineClean.startsWith("MSH") ) {
                        mshCount++
                        if (mshCount > 1 ) {
                            provenance.singleOrBatch = Provenance.BATCH_FILE
                            println("Sending $provenance")
                            println("Message: ${currentLinesArr.joinToString("\n")}")
                            provenance.messageIndex++
                        }

                        currentLinesArr.clear()
                    } // .if
                    currentLinesArr.add(lineClean)
                } // .else
            }
            println("Sending last one: $provenance")
            println("Last Message: ${currentLinesArr.joinToString("\n")}")
        }
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