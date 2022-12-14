
import gov.cdc.dex.hl7.receiver.ReceiverProcessMetadata
import gov.cdc.dex.metadata.*
import gov.cdc.dex.util.DateHelper.toIsoString
import org.junit.jupiter.api.Test
import java.util.*

class TestLineCleanup {

    @Test
    fun testLineTrim() {
        val line = "   abc   "
        val lineClean = line.trim().let { if ( it.startsWith("a") )  it.substring(1)  else it}
        println(lineClean)
        assert(lineClean == "bc")
    }

//    @Test
//    fun testFullMetadataObject() {
//        //Provenance:
//        val provenance= Provenance(
//            eventId ="123",
//            eventTimestamp = "2022-11-17",
//            filePath="abfss://container@storage/folder/file.txt",
//            fileTimestamp="2022-10-01T12:00:00.0Z",
//            fileSize=1000,
//            singleOrBatch = Provenance.SINGLE_FILE,
//            systemProvider = "Unit Test",
//            originalFileName = "localFile.txt"
//        )
//
//        val problem = Problem("UNIT-TEST", "java.lang.Exception", null, "Mock error for unit test", false, 0, 1)
//        val summary = SummaryInfo("UNIT-TEST-ERROR", problem)
//
//        val processMD = ReceiverProcessMetadata("SUCCESS")
//        processMD.startProcessTime = Date().toIsoString()
//        processMD.endProcessTime = Date().toIsoString()
////        processMD.oneMoreProperty = "Test Metadata ONE"
//
//
//        val metadata = DexMetadata(provenance, listOf(processMD))
//        val event = DexEventPayload("MSH|...", metadata, summary)
//
//        println(event)
//        println("------")
//        println(provenance.messageHash)
//    }
}