import com.google.gson.JsonObject
import gov.cdc.dex.hl7.*
import gov.cdc.dex.azure.EventHubMetadata
import java.io.File
import java.util.ArrayList
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries
import local.*

object RunClass {
    val evHubOkName: String = System.getenv("EventHubSendOkName")
    val evHubErrorName: String = System.getenv("EventHubSendErrsName")
    val evHubConnStr = System.getenv("EventHubConnectionString")
    val containerPath = System.getenv("ContainerPath")

    @JvmStatic
    fun main(args: Array<String>) {
        val container = System.getenv("EventHubConnectionString")
        val containerName = System.getenv("EventHubReceiveNameCASE")
        val eventHubMDList: MutableList<EventHubMetadata> = ArrayList()
        val messages = Path("$container/$containerName")
            .listDirectoryEntries("*.txt")
            .mapIndexed { index, path ->
                eventHubMDList.add(EventHubMetadata(index, 99, "", ""))
                File(path.absolutePathString()).readText()
            }

        if (messages.isNotEmpty()) {
            val ok = OutBinding<List<String>>()
            val err = OutBinding<List<String>>()
            val cosmos = OutBinding<List<JsonObject>>()
            with(Function()) {
                eventHubProcessor( messages, eventHubMDList, context, ok, err, cosmos)
            }
            ok.value?.let { writeToHub( evHubConnStr, evHubOkName, it) }
            err.value?.let { writeToHub( evHubConnStr, evHubErrorName, it) }
            cosmos.value?.let {writeToContainer(containerPath, it) }
        }
        else {
            println(" No messages found")
        }
    }
}
