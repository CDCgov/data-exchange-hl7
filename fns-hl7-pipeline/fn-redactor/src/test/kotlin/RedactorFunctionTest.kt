import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.Function
import gov.cdc.dex.metadata.ProcessMetadata
import gov.cdc.dex.metadata.SummaryInfo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.junit.jupiter.api.Assertions
import org.mockito.internal.matchers.Null
import java.io.File
import java.util.*
import java.util.logging.Logger

class RedactorFunctionTest {

    @Test
    fun process_HappyPath() {
        println("Starting process_HappyPath test")
        val function = Function()
        val text = File("src/test/resources/ELR_message.txt").readText()

        val messages: MutableList<String> = ArrayList()
        messages.add(text)
        val eventHubMDList: MutableList<EventHubMetadata> = ArrayList()
        val eventHubMD = EventHubMetadata(1, 99, "", "")
        eventHubMDList.add(eventHubMD)
        val inputEvent : JsonObject = function.eventHubProcessor(messages, eventHubMDList, getExecutionContext()!!)

        val summaryObj : JsonObject? = inputEvent.get("summary").asJsonObject
        // Validate current_status is successful
        if (summaryObj != null) {
            Assertions.assertEquals("REDACTED", summaryObj.get("current_status").asString)
        }
        // Validate process Object is valid
        val metadata = inputEvent.get("metadata")
        kotlin.io.println("metadata: $metadata")

    }

    @Test
    fun process_ExceptionPath() {
        println("Starting processELR_HappyPath test")
        val function = Function()
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

    }


    @Test
    fun invoke_test(){
        val function = Function()
        val req: HttpRequestMessage<Optional<String>> = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
        try {
            function.invoke(req, getExecutionContext()!!)
        } catch (e : NullPointerException) {
            println("function invoked, message body is null")
        }
        assert(true)
    }

    private fun getExecutionContext(): ExecutionContext? {
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