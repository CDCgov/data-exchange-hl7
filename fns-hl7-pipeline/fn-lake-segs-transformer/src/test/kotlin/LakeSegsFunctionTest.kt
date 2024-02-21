import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.Function
import org.junit.jupiter.api.Test
import java.util.logging.Logger
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.microsoft.azure.functions.OutputBinding
import org.junit.jupiter.api.Assertions

public class LakeSegsFunctionTest {

    private fun processFile(filename:String, isHappyPath:Boolean) {
        println("Start processing $filename ")
        val text = this:: class.java.getResource("/$filename").readText()
        val messages = listOf(text)
        val eventHubMDList = listOf(EventHubMetadata(1, 99, "", ""))
        val function = Function()
        val inputEvents : List<String> = function.eventHubProcessor(messages, eventHubMDList)
        val inputEvent : JsonObject = JsonParser.parseString(inputEvents[0]).asJsonObject
        // Validate that 'stage' has been added to metadata
        val metadata: JsonObject? = inputEvent.get("metadata").asJsonObject

        if(metadata != null){
            val stage = metadata.get("stage")?.asJsonObject
            Assertions.assertTrue(stage != null)
        }

        // Validate that 'processes' does not exist in metadata
        if (metadata !=  null) {
            val processes = metadata.get("processes")?.asJsonArray
            Assertions.assertTrue(processes == null)
        }
        val summaryObj : JsonObject? = inputEvent.get("summary").asJsonObject
        println("the summary object: $summaryObj")
        if (summaryObj != null){
            if(isHappyPath){
                // Validate Summary.current_status is successful
                Assertions.assertEquals("LAKE-SEGMENTS-TRANSFORMED", summaryObj.get("current_status").asString)
            }
            else{
                // Validate current_status is unsuccessful
                Assertions.assertEquals("LAKE-SEGMENTS-ERROR", summaryObj.get("current_status").asString)
            }
        }

        println("Finished processing $filename ")
    }

    @Test
    fun processELR_HappyPath() {
        processFile("ELR_message.txt", true)
        assert (true)
    }

    @Test
    fun processCASE_HappyPath() {
        processFile("CASE_message.txt", true)
        assert (true)
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

    private fun getExecutionContext():
            ExecutionContext {
        return object :ExecutionContext {
            override fun getLogger():Logger {
                return Logger.getLogger(Function:: class.java.name)
            }

            override fun getInvocationId():String {
                return "null"
            }

            override fun getFunctionName():String {
                return "null"
            }
        }
    }
}