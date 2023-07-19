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

    private fun processFile(filename:String, isHappyPath:Boolean) {

        println("Start processing $filename ")
        val text = this:: class.java.getResource("/$filename").readText()
        val messages = listOf(text)
        val eventHubMDList = listOf(EventHubMetadata(1, 99, "", ""))
        val function = Function()
        val inputEvent : JsonObject = function.eventHubProcessor(messages, eventHubMDList, getExecutionContext())

        // Validate Metadata.processes has been added to the array of proccesses
        val metadata: JsonObject? = inputEvent.get("metadata").asJsonObject
        if(metadata != null){
            val processes: JsonArray? = metadata.get("processes").asJsonArray
            Assertions.assertTrue(processes != null)
        }

        val summaryObj : JsonObject? = inputEvent.get("summary").asJsonObject
        if (summaryObj != null){
            if(isHappyPath){
                Assertions.assertEquals("SUCCESS", summaryObj.get("current_status").asString)
            }
            else{
                Assertions.assertEquals("FAILURE", summaryObj.get("current_status").asString)
            }
        }

        println("Finished processing $filename ")
    }

    @Test
    fun process_HappyPath() {
        processFile("ELR_message.txt", true)
        assert(true)
    }

    @Test
    fun process_ExceptionPath() {
        processFile("Error_message.txt", false)
        assert(true)
        
    }

    // @Test
    // fun invoke_test(){
    //     val function = Function()
    //     val req: HttpRequestMessage<Optional<String>> = mock(HttpRequestMessage::class.java) as HttpRequestMessage<Optional<String>>
    //     try {
    //         function.invoke(req, getExecutionContext()!!)
    //     } catch (e : NullPointerException) {
    //         println("function invoked, message body is null")
    //     }
    //     assert(true)
    // }

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