package gov.cdc.dex

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.validation.structure.ValidatorFunction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import java.util.logging.Logger
import org.mockito.Mockito.mock
import java.io.File
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import org.junit.jupiter.api.Assertions

class ValidatorFunctionTest {

    private fun processFile(filename: String) {
        println("Start processing $filename ")
        val text = this::class.java.getResource("/$filename").readText()
        val messages = listOf(text)

        val eventHubMDList = listOf(EventHubMetadata(1, 99, "", ""))

        val function = ValidatorFunction()
        function.run(messages, eventHubMDList, getExecutionContext())
        println("Finished processing $filename ")

    }

    @Test
    fun processELR_HappyPath() {
        processFile("ELR_message.txt")
        assert(true)
    }

    @Test
    fun processELR_ExceptionPath() {
        processFile("ELR_Exceptionmessage.txt")
        assert(true)
    }

    @Test
    fun processCASE_HappyPath() {
        processFile("CASE_message.txt")
        assert(true)
    }

    @Test
    fun invoke_test(){
        val function = ValidatorFunction()
        val req: HttpRequestMessage<Optional<String>> = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        assertThrows<NullPointerException> { function.invoke(req) }
    }

    @Test
    fun process_HappyPath() {
        println("Starting process_HappyPath test")
        val function = ValidatorFunction()
        val text = File("src/test/resources/ELR_message.txt").readText()

        val messages: MutableList<String> = ArrayList()
        messages.add(text)
        val eventHubMDList: MutableList<EventHubMetadata> = ArrayList()
        val eventHubMD = EventHubMetadata(1, 99, "", "")
        eventHubMDList.add(eventHubMD)
        val inputEvent : JsonObject = function.eventHubProcessor(messages, eventHubMDList, getExecutionContext()!!)

        // Validate Summary.current_status is successful
        val summaryObj : JsonObject? = inputEvent.get("summary").asJsonObject
        if (summaryObj != null) {
            Assertions.assertEquals("SUCCESS", summaryObj.get("current_status").asString)
        }

        // Validate Process Metadata has been added to the array of proccesses
        val jarr: JsonArray? = inputEvent.get("processes").asJsonArray
        if(jarr != null){
            val item = jarr.getJSONObject(0)
            Assertions.assertTrue(item.get("metadata") != null)
        }
    }

    @Test
    fun process_ExceptionPath() {
        println("Starting processELR_Exception Path test")
        val function = ValidatorFunction()
        val text = File("src/test/resources/Error_message.txt").readText()

        val messages: MutableList<String> = ArrayList()
        messages.add(text)
        val eventHubMDList: MutableList<EventHubMetadata> = ArrayList()
        val eventHubMD = EventHubMetadata(1, 99, "", "")
        eventHubMDList.add(eventHubMD)
        val inputEvent = function.eventHubProcessor(messages, eventHubMDList, getExecutionContext()!!)

        val summaryObj : JsonObject? = inputEvent.get("summary").asJsonObject
        // Validate current_status is unsuccessful
        if (summaryObj != null) {
            Assertions.assertEquals("FAILURE", summaryObj.get("current_status").asString)
        }
        // Validate Process MD w/ appropriate assertion? > metadata > processes
        // Validate Process Metadata has been added to the array of proccesses
        val jarr: JsonArray? = inputEvent.get("processes").asJsonArray
        if(jarr != null){
            val item = jarr.getJSONObject(0)
            Assertions.assertTrue(item.get("metadata") != null)
        }

    }

    private fun getExecutionContext(): ExecutionContext {
        return object : ExecutionContext {
            override fun getLogger(): Logger {
                return Logger.getLogger(ValidatorFunction::class.java.name)
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