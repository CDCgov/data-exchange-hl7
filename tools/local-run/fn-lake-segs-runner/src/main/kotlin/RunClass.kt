import gov.cdc.dex.hl7.*
import gov.cdc.dex.azure.EventHubMetadata
import java.io.File
import java.util.*
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries


object RunClass {
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
            val func = Function()
            func.eventHubCASEProcessor(messages, eventHubMDList, context)
        } else {
            println("No messages found")
        }
    }
}

