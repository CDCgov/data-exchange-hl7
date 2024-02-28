
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.model.StructureValidatorStageMetadata
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper.toJsonElement
import org.junit.jupiter.api.Test
import java.util.*

class EventHubPayloadTest {

    @Test
    fun testGeneratePayload() {
        val eventInput = this::class.java.getResource("/mockEventHubPayload.json").readText()
        val root: JsonObject = JsonParser.parseString(eventInput) as JsonObject

        val ehMD = EventHubMetadata(1, 1, null, "2023-01-01")
        val processMD = StructureValidatorStageMetadata("SUCCESS", null, ehMD, listOf())
        processMD.startProcessTime = Date().toIsoString()
        processMD.endProcessTime = Date().toIsoString()

        val newPayload = JsonObject()
        newPayload.add("metadata", JsonObject())
        newPayload["metadata"].asJsonObject.add("stage", processMD.toJsonElement())
        println(newPayload)

    }
}