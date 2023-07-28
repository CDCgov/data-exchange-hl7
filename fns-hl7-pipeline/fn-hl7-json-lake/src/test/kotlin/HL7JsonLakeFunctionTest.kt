import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.Function
import org.junit.jupiter.api.Test
import java.util.logging.Logger
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import org.junit.jupiter.api.Assertions

public class HL7JsonLakeFunctionTest {

    private fun processFile(filename:String, isCase:Boolean, isHappyPath:Boolean) {
        println("Start processing $filename ")
        val text = this:: class.java.getResource("/$filename").readText()
        val messages = listOf(text)
        val eventHubMDList = listOf(EventHubMetadata(1, 99, "", ""))
        val function = Function()
        val inputEvent : JsonObject

        inputEvent = function.eventHubCASEProcessor(messages, eventHubMDList, getExecutionContext())
        // inputEvent = function.eventHubELRProcessor(messages, eventHubMDList, getExecutionContext())

        // Validate Metadata.processes has been added to the array of proccesses
        val metadata: JsonObject? = inputEvent.get("metadata").asJsonObject
        if(metadata != null){
            val processes: JsonArray? = metadata.get("processes").asJsonArray
            Assertions.assertTrue(processes != null)
        }
        val summaryObj : JsonObject? = inputEvent.get("summary").asJsonObject
        if (summaryObj != null){
            if(isHappyPath){
                // Validate Summary.current_status is successful
                Assertions.assertEquals("HL7-JSON-LAKE-TRANSFORMED", summaryObj.get("current_status").asString)
            }
            else{
                // Validate current_status is unsuccessful
                Assertions.assertEquals("HL7-JSON-LAKE-ERROR", summaryObj.get("current_status").asString)
            }
        }

        println("Finished processing $filename ")
    }

    @Test
    fun processELR_HappyPath() {
        processFile("ELR_message.txt", false, true)
        assert (true)
    }

    @Test
    fun processELR_ExceptionPath() {
        processFile("ELR_Exceptionmessage.txt", false, false)
        assert (true)
    }

    @Test
    fun processCASE_HappyPath() {
        processFile("CASE_message.txt", true, true)
        assert (true)
    }

    @Test
    fun processCASE_ExceptionPath() {
        processFile("Exceptionmessage.txt", true, false)
        assert (true)
    }

    private fun getExecutionContext():ExecutionContext {
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
