import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.receiver.Function
import org.junit.jupiter.api.Test
import java.io.File
import java.util.logging.Logger

class FunctionTest {

    @Test
    fun callReceiverDebatcherFunction_happyPath() {
        println("Starting Function test")
        val function = Function()
        val text = File("src/test/resources/message.txt").readText()
        //JsonParser.parseString(text)

        val messages: MutableList<String> = ArrayList()
            messages.add(text)
            val eventHubMDList: MutableList<EventHubMetadata> = ArrayList()
            val eventHubMD = EventHubMetadata(1, 99, "", "")
            eventHubMDList.add(eventHubMD)
            function.eventHubProcessor(messages, eventHubMDList, getExecutionContext()!!)
        println("Finished Function test")
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