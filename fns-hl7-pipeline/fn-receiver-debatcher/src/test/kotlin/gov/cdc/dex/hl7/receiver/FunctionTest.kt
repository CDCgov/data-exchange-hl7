package gov.cdc.dex.hl7.receiver

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import org.junit.jupiter.api.Test

import java.util.logging.Logger

class FunctionTest {

    private fun processFile(filename: String) {
        println("Start processing $filename ")
        val text = this::class.java.getResource("/$filename").readText()
        val messages = listOf(text)

        val eventHubMDList = listOf(EventHubMetadata(1, 99, "", ""))

        val function = Function()
        function.eventHubProcessor(messages, eventHubMDList, getExecutionContext())
        println("Finished processing $filename ")
        assert(true)

    }

    @Test
    fun processELR_HappyPath() {
        processFile("ELR_message.txt")
    }
    @Test
    fun processCASE_HappyPath() {
        processFile("CASE_message.txt")
    }
    @Test
    fun process_ErrorPath() {
        processFile("ERROR_message.txt")
    }
    @Test
    fun process_NoMetadata() {
        processFile("NoMetadataFile.txt")
    }
    @Test
    fun process_BatchMessage() {
        processFile("BatchMessage.txt")
    }
    @Test
    fun process_InvalidMessage() {
        processFile("InvalidMessage.txt")
    }

    @Test
    fun process_ELRWithBlanks() {
        processFile("CovidELRWithBlanks.txt")
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