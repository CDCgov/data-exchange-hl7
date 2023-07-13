import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.Function
import org.junit.jupiter.api.Test
import java.util.logging.Logger

public class LakeSegsFunctionTest {

    private fun processFile(filename:String, isCase:Boolean) {
        println("Start processing $filename ")
        val text = this:: class.java.getResource("/$filename").readText()
        val messages = listOf(text)
        val eventHubMDList = listOf(EventHubMetadata(1, 99, "", ""))
        val function = Function()

        if(isCase){
           return function.eventHubCASEProcessor(messages, eventHubMDList, getExecutionContext())
        }else{
           return function.eventHubELRProcessor(messages, eventHubMDList, getExecutionContext())
        }
        //println("Finished processing $filename ")
    }

    @Test
    fun processELR_HappyPath() {
        var summary = processFile("ELR_message.txt", false)
        assertThat(summary.current_status, "LAKE-SEGMENTS-TRANSFORMED")
    }

    @Test
    fun processELR_ExceptionPath() {
        var summary = processFile("ELR_Exceptionmessage.txt", false)
        asserThat(summary.current_status, "LAKE-SEGMENTS-ERROR")
        assertNotNull(summary.problem)
        
    }

    @Test
    fun processCASE_HappyPath() {
        var summary = processFile("CASE_message.txt", true)
        asserThat(summary.current_status, "LAKE-SEGMENTS-TRANSFORMED")
    }

    @Test
    fun processCASE_ExceptionPath() {
        var summary = processFile("ELR_Exceptionmessage.txt", true)
        asserThat(summary.current_status, "LAKE-SEGMENTS-ERROR")
        assertNotNull(summary.problem)
    }

    @Test
    fun process_ErrorPath() {
        var summary = processFile("Exceptionmessage.txt", true)
        asserThat(summary.current_status, "LAKE-SEGMENTS-ERROR")
        assertNotNull(summary.problem)
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
