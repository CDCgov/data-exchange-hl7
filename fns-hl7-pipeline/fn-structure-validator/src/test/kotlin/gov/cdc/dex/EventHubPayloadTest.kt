package gov.cdc.dex

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import gov.cdc.dex.model.StructureValidatorProcessMetadata
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper.addArrayElement
import org.junit.jupiter.api.Test
import java.util.*

class EventHubPayloadTest {

    @Test
    fun testGeneratePayload() {
        val eventInput = this::class.java.getResource("/mockEventHubPayload.json").readText()
        val root: JsonObject = JsonParser.parseString(eventInput) as JsonObject

        val processMD = StructureValidatorProcessMetadata("SUCCESS", null)
        processMD.startProcessTime = Date().toIsoString()
        processMD.endProcessTime = Date().toIsoString()

        //Validate a message to test report to Json
//        val testMessage = this::class.java.getResource("/Invalid-GenV1-0-Case-Notification.hl7").readText()
////        val phinSpec = HL7StaticParser.getFirstValue(testMessage, "MSH-21[1].1").get()
//        val phinSpec = "NND_ORU_V2.0"
//        val nistValidator = ProfileManager(ResourceFileFetcher(), "/$phinSpec")
//
//        processMD.report =  nistValidator.validate(testMessage)

        val newPayload = JsonObject()
        newPayload.add("metadata", JsonPrimitive("abc"))
        newPayload.addArrayElement("processes", processMD)
        println(newPayload)

        val secondProcessMD = StructureValidatorProcessMetadata( "SUCCESS", null)
        secondProcessMD.startProcessTime = Date().toIsoString()
        secondProcessMD.endProcessTime = Date().toIsoString()

//        val currentProcessPayload = newPayload["processes"].asJsonArray
        newPayload.addArrayElement("processes", secondProcessMD)
        println("After second Process\n==========")
        println(newPayload)
    }
}