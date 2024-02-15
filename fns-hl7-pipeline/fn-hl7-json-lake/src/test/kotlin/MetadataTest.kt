import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.HL7JSONLakeProcessMetadata
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.bumblebee.HL7JsonTransformer
import org.junit.jupiter.api.Test
import java.util.*


class MetadataTest {

    private val PROFILE_FILE_PATH = "PhinGuideProfile_v2.json"
    private val gsonNoNulls: Gson = GsonBuilder().create()

    @Test
    fun testStageMetadata() {
        // test replacing previous 'stage' element with current one
        val text = this::class.java.getResource("hl7Messages/hl7Message.txt").readText()
        val bumblebee = HL7JsonTransformer.getTransformerWithResource(text, PROFILE_FILE_PATH)
        val jsonObject = bumblebee.transformMessage()
        // load test json as if it were metadata coming from previous process
        val testMessage = this::class.java.getResource("test.json").readText()
        val inputEvent = JsonParser.parseString(testMessage)
        val metadata = JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject
        val processMD = HL7JSONLakeProcessMetadata(
            status = "SUCCESS",
            output = gsonNoNulls.toJsonTree(jsonObject).asJsonObject,
            eventHubMD = EventHubMetadata(1, 99, "", ""),
            config = listOf(PROFILE_FILE_PATH))
        processMD.startProcessTime = Date().toIsoString()
        processMD.endProcessTime = Date().toIsoString()
        // overwrite existing stage
        metadata.add("stage", processMD.toJsonElement())
        println(inputEvent)
    }

}