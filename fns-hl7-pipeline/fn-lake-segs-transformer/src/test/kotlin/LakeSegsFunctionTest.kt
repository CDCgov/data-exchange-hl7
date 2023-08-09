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
            function.eventHubCASEProcessor(messages, eventHubMDList, getExecutionContext())
        }else{
            function.eventHubELRProcessor(messages, eventHubMDList, getExecutionContext())
        }
        println("Finished processing $filename ")
    }

    @Test
    fun processELR_HappyPath() {
        processFile("ELR_message.txt", false)
        assert (true)
    }

    @Test
    fun processELR_ExceptionPath() {
        processFile("ELR_Exceptionmessage.txt", false)
        assert (true)
    }

    @Test
    fun processCASE_HappyPath() {
        processFile("CASE_message.txt", true)
        assert (true)
    }

    @Test
    fun processCASE_ExceptionPath() {
        processFile("ELR_Exceptionmessage.txt", true)
        assert (true)
    }

    @Test
    fun process_ErrorPath() {
        processFile("Exceptionmessage.txt", true)
        assert (true)
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
