import com.google.gson.JsonObject
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.MMGValidationFunction
import gov.cdc.dex.hl7.model.ReportStatus
import gov.cdc.dex.util.JsonHelper
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

import java.util.logging.Logger
import kotlin.test.*
@Tag("IntegrationTest")
class ITFunctionTest {

    private fun processFile(filename: String): JsonObject {
        println("Start processing $filename ")
        val text = this::class.java.getResource("/$filename").readText()
        val messages = listOf(text)
        val eventHubMDList = listOf(EventHubMetadata(1, 99, "", ""))

        val function = MMGValidationFunction()
        return function.eventHubProcessor(messages, eventHubMDList, getExecutionContext())
    }

    @Test
    fun process_HappyPath() {
        val output = processFile("validator-msg.txt")
        assertNotNull(output, "Asserting that non-null result is returned from eventHubProcessor")
        val processes = JsonHelper.getValueFromJson("metadata.processes", output).asJsonArray
        val filtered = processes.filter { it.asJsonObject["process_name"].asString == "MMG-VALIDATOR" }
        assertTrue(filtered.isNotEmpty(), "Asserting process named MMG-VALIDATOR exists")
        if (filtered.isNotEmpty()) {
             assertEquals(
                ReportStatus.MMG_VALID.toString(),
                filtered[0].asJsonObject["status"].asString,
                "Asserting status == ${ReportStatus.MMG_VALID}"
            )
        }
        val outputSummary = output.asJsonObject["summary"].asJsonObject
        assertEquals(
            ReportStatus.MMG_VALID.toString(),
            outputSummary["current_status"].asString,
            "Asserting summary.current_status == ${ReportStatus.MMG_VALID}"
        )
        assertTrue(outputSummary["problem"].isJsonNull, "Asserting summary.problem is JsonNull")

    }

    @Test
    fun process_ErrorPath() {
        val output = processFile("InvalidMessage.txt") //content is Lyme_WithMissingOBR31
        assertNotNull(output, "Asserting that non-null result is returned from eventHubProcessor")
        val processes = JsonHelper.getValueFromJson("metadata.processes", output).asJsonArray
        val filtered = processes.filter { it.asJsonObject["process_name"].asString == "MMG-VALIDATOR" }
        assertTrue(filtered.isNotEmpty(), "Asserting process named MMG-VALIDATOR exists")
        if (filtered.isNotEmpty()) {
            assertEquals(
                "MMG_VALIDATOR_EXCEPTION",
                filtered[0].asJsonObject["status"].asString,
                "Asserting status == MMG_VALIDATOR_EXCEPTION"
            )
        }
        val outputSummary = output.asJsonObject["summary"].asJsonObject
        assertEquals(
            "ERROR", outputSummary["current_status"].asString,
            "Asserting summary.current_status == ERROR",
        )
        assertFalse(outputSummary["problem"].isJsonNull, "Asserting summary.problem is NOT JsonNull")
    }

    private fun getExecutionContext(): ExecutionContext {
        return object : ExecutionContext {
            override fun getLogger(): Logger {
                return Logger.getLogger(ITFunctionTest::class.java.name)
            }

            override fun getInvocationId(): String {
                return "null"
            }

            override fun getFunctionName(): String {
                return "null"
            }
        }
    }

}