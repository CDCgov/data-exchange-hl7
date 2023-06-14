import com.google.gson.JsonObject
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.MMGValidationFunction
import gov.cdc.dex.util.JsonHelper
import org.junit.jupiter.api.Test

import java.util.logging.Logger
import kotlin.test.*

class FunctionTest {

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
        var output = processFile("validator-msg.txt")
        assertNotNull(output)
        assertEquals("MMG_VALID", JsonHelper.getValueFromJson("summary.current_status", output).asString)
        assertTrue(JsonHelper.getValueFromJson("summary.problem", output).isJsonNull)
    }

    @Test
    fun process_ErrorPath() {
        var output = processFile("InvalidMessage.txt")
        assertNotNull(output)
        assertNotEquals("MMG_VALID", JsonHelper.getValueFromJson("summary.current_status", output).asString)
        //assertTrue(JsonHelper.getValueFromJson("summary.problem", output).isJsonNull)
    }

    private fun getExecutionContext(): ExecutionContext {
        return object : ExecutionContext {
            override fun getLogger(): Logger {
                return Logger.getLogger(FunctionTest::class.java.name)
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