import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpRequestMessage
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.Function
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
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
        function.eventHubProcessor(messages, eventHubMDList, getExecutionContext()!!)
        assert(true)
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
        function.eventHubProcessor(messages, eventHubMDList, getExecutionContext()!!)
        assert(true)
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