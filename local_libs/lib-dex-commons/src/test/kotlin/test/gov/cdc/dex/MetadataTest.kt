package test.gov.cdc.dex

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import gov.cdc.dex.metadata.*
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper.addArrayElement
import org.junit.jupiter.api.Test
import test.MockMetadata
import java.util.*

class MetadataTest {

    @Test
    fun testGeneratePayloadJson() {

        val processMD = MockMetadata("SUCCESS")
        processMD.startProcessTime = Date().toIsoString()
        processMD.endProcessTime = Date().toIsoString()
        processMD.oneMoreProperty = "Test Metadata ONE"

        val newPayload = JsonObject()
        val md = JsonObject()
        md.addProperty("prop", "abc")
        newPayload.add("metadata", md)
        val metadata = newPayload["metadata"] as JsonObject
        metadata.addArrayElement("processes", processMD)

        val secondProcessMD = MockMetadata( "SUCCESS")
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
    fun testFullMetadataObject() {
        //Provenance:
        val provenance= Provenance(
            filePath= "abfss://container@storage/folder/file.txt",
            fileTimestamp = "2022-10-01T12:00:00.0Z",
            fileSize = 1000,
            singleOrBatch = Provenance.SINGLE_FILE,
            messageHash = "1234",
            systemProvider = "Unit Test",
            originalFileName = "localFile.txt"
        )

        val problem = Problem("UNIT-TEST", "java.lang.Exception", null, "Mock error for unit test", false, 0, 1)
        val summary = SummaryInfo("UNIT-TEST-ERROR", problem)

        val processMD = MockMetadata("SUCCESS")
        processMD.startProcessTime = Date().toIsoString()
        processMD.endProcessTime = Date().toIsoString()
        processMD.oneMoreProperty = "Test Metadata ONE"


        val metadata = DexMetadata(provenance, listOf(processMD))
        val event = DexEventPayload("MSH|...", metadata, summary)

        println(event)
    }
}