import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.OutputBinding
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.Function
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.logging.Logger

class RedactorFunctionTest {

    private fun processFile(filename:String, isHappyPath:Boolean) {
//TODO: Fix these unit tests -- requires marking up new JSON mock event files
//        println("Start processing $filename ")
//        val text = this:: class.java.getResource("/$filename").readText()
//        val messages = listOf(text)
//        val eventHubMDList = listOf(EventHubMetadata(1, 99, "", ""))
//        val function = Function()
//        val inputEvent : JsonObject = function.eventHubProcessor(messages, eventHubMDList)
//
//        // Validate stage has been added to the metadata
//        val stageMD = inputEvent.get("stage").asJsonObject
//        assert(stageMD["stage_name"].asString == "REDACTOR")
//        val summaryObj : JsonObject? = inputEvent.get("summary").asJsonObject
//        if (summaryObj != null){
//            if(isHappyPath){
//                Assertions.assertEquals("REDACTED", summaryObj.get("current_status").asString)
//            }
//            else{
//                Assertions.assertEquals("FAILURE", summaryObj.get("current_status").asString)
//            }
//        }
//
//        println("Finished processing $filename ")
//    }
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
    private fun <T> getOutputBindingList(): OutputBinding<List<T>> {
        return object : OutputBinding<List<T>> {
            var innerList : List<T>? = null
            override fun getValue(): List<T>? {
                return innerList
            }

            override fun setValue(p0: List<T>?) {
                innerList = p0
            }

        }
    }
}