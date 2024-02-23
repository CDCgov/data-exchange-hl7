package test.gov.cdc.dex


import com.google.gson.GsonBuilder
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.*
import gov.cdc.dex.util.DateHelper.toIsoString
import org.junit.jupiter.api.Test
import java.util.*


class MetadataTest {
    val gson = GsonBuilder().serializeNulls().create()
        //.addSerializationExclusionStrategy(SuperclassExclusionStrategy()).addDeserializationExclusionStrategy(SuperclassExclusionStrategy()).create()


    private fun getEventHubStageMDInstance(): EventHubStageMetadata {
        val eventHubMD  = EventHubMetadata(1,1,null, "20230101")
        val processMD = MockEventHubStageMetadata("TEST-STAGE", getPOMVersion(), "UNIT-TEST", listOf(),eventHubMD)
        processMD.startProcessTime = Date().toIsoString()
        processMD.endProcessTime = Date().toIsoString()
        return processMD
    }

    private fun getEventGridStageMDInstance(): EventGridStageMetadata {
        val processMD = MockEventGridStageMetadata(
            "TEST-STAGE",
            getPOMVersion(),
            "UNIT_TEST",
            listOf(),
            "20240216T09:59:00"
        ){}
        processMD.startProcessTime = Date().toIsoString()
        processMD.endProcessTime = Date().toIsoString()
        return processMD
    }

    private fun getPOMVersion(): String {
//        val reader: MavenXpp3Reader = MavenXpp3Reader()
//        val model: Model = reader.read(FileReader("pom.xml"))
//        return this::class.java.getPackage().implementationVersion
        return "0.0.1"
    }

    private fun getSummaryInfoInstance():SummaryInfo {
        return SummaryInfo("UNIT-TEST-ERROR", getProblemInstance())
    }
    private fun getProblemInstance() = Problem("UNIT-TEST", "java.lang.Exception", null, "Mock error for unit test", false, 0, 1)


    @Test
    fun testV2Metadata() {
//        val tracing = DEXTracingHeader("traceID", "parentSpan")
//        val dataStream = DEXDataStream("dataStreamID", "dataStreamRoute")
//        val provenance = DEXProvenance("senderID", "system", "filePath", "fileTS", 10, null)
//        val fileMD = DEXFileMetadata(reportingJurisdiction = "13", uploadID = "", provenance = provenance, dataStream = dataStream, tracing = tracing)

        val routingInfo = RoutingMetadata("filePPath", "fileTS", 10, "orgA", "uploadID", "dsid", "dsroute", "trace", "span")
        val stageMD = MockEventGridStageMetadata("stgName", "stgVersion", "sttus", listOf(), "ts")
        val messageMD = MessageMetadata("msgUUID","SINGLE", 1, "hash", "MSH|")
        val summary = SummaryInfo("All good!")
        val fullMD = DexHL7Metadata(messageMetadata = messageMD, routingInfo = routingInfo, stage = stageMD, summary = summary)

        val json = gson.toJson(fullMD)
        println(json)

    }

}