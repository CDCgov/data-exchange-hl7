package test.gov.cdc.dex

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.*
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper.addArrayElement

import org.junit.jupiter.api.Test
import test.MockMetadata

import java.util.*


class MetadataTest {
    val gson = GsonBuilder().serializeNulls().create()
        //.addSerializationExclusionStrategy(SuperclassExclusionStrategy()).addDeserializationExclusionStrategy(SuperclassExclusionStrategy()).create()

    @Test
    fun testGeneratePayloadJson() {
        val eventHubMD = EventHubMetadata(1,1,null, "20230101")
        val processMD = MockMetadata("SUCCESS", eventHubMD )

        processMD.startProcessTime = Date().toIsoString()
        processMD.endProcessTime = Date().toIsoString()
        processMD.oneMoreProperty = "Test Metadata ONE"

        val newPayload = JsonObject()
        val md = JsonObject()
        md.addProperty("prop", "abc")
        newPayload.add("metadata", md)
        val metadata = newPayload["metadata"] as JsonObject
        metadata.addArrayElement("processes", processMD)


        val secondProcessMD = MockMetadata( "SUCCESS", eventHubMD)
        secondProcessMD.startProcessTime = Date().toIsoString()
        secondProcessMD.endProcessTime = Date().toIsoString()

//        val currentProcessPayload = newPayload["processes"].asJsonArray
        metadata.addArrayElement("processes", secondProcessMD)
        println("After second Process\n==========")

        val summaryOne = JsonObject()
        summaryOne.addProperty("current_status", "Test One")
        metadata.add("summary", summaryOne)

        val summaryTwo = JsonObject()
        summaryTwo.addProperty("current_status", "Test TWO")
        metadata.add("summary", summaryTwo)

        println(metadata)
    }

    @Test
    fun testFullMetadataObjectForEventHub() {
        val event = DexEventPayload(
            messageInfo = getDexMDInstance(),
            metadata = DexMetadata(getProvenanceInstance(), getEventHubStageMDInstance()),
            summary = getSummaryInfoInstance(),
            content = "",
            routingMetadata = getRoutingMDInstance()
        )
        val mdJson = gson.toJson(event)
        println(mdJson)

    }

    @Test
    fun testFullMetadataObjectForEventGrid() {
        val event = DexEventPayload(
            messageInfo = getDexMDInstance(),
            metadata = DexMetadata(getProvenanceInstance(), getEventGridStageMDInstance()),
            summary = getSummaryInfoInstance(),
            content = "",
            routingMetadata = getRoutingMDInstance()
        )
        val mdJson = gson.toJson(event)
        println(mdJson)

    }

    private fun getRoutingMDInstance() =
        RoutingMetadata(
            uploadID = null,
            traceID = null,
            parentSpanID = null,
            destinationEvent = null,
            destinationID = null
        )

    private fun getDexMDInstance() = DexMessageInfo(null, null, null, null, HL7MessageType.CASE)

    private fun getEventHubStageMDInstance(): EventHubStageMetadata {
        val eventHubMD  = EventHubMetadata(1,1,null, "20230101")
        val processMD =object: EventHubStageMetadata("TEST-STAGE", getPOMVersion(), "UNIT-TEST", listOf(),eventHubMD){}
         processMD.startProcessTime = Date().toIsoString()
        processMD.endProcessTime = Date().toIsoString()
        return processMD
    }

    private fun getEventGridStageMDInstance(): EventGridStageMetadata {
        val processMD = object: EventGridStageMetadata(
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

    private fun getProvenanceInstance(): Provenance {
        return Provenance(
            eventId = "test",
            eventTimestamp = "2022-11-17",
            filePath = "abfss://container@storage/folder/file.txt",
            fileTimestamp = "2022-10-01T12:00:00.0Z",
            fileSize = 1000,
            singleOrBatch = Provenance.SINGLE_FILE,
            messageHash = "1234",
            systemProvider = "Unit Test",
            originalFileName = "localFile.txt",
            originalFileTimestamp = "2022-10-01T12:00:00.0Z"
        )

    }

//    @Test
//    fun testNullValuesInJson() {
//        val elem: JsonElement = JsonNull.INSTANCE
//
//        print(elem)
//        //try {
//        val str = elem.asString
//    }
}