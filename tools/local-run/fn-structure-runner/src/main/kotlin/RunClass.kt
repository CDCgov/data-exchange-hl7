import gov.cdc.dex.hl7.validation.structure.*
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
        var container = System.getenv("EventHubConnectionString")
        var containerName = System.getenv("EventHubReceiveName")

        val eventHubMDList: MutableList<EventHubMetadata> = ArrayList()
        val messages = Path("$container/$containerName")
            .listDirectoryEntries("*.txt")
            .mapIndexed { index, path ->
                eventHubMDList.add(EventHubMetadata(index, 99, "", ""))
                File(path.absolutePathString()).readText()
            }
        if ( messages.isNotEmpty() ) {
            with(ValidatorFunction()) { eventHubProcessor(messages, eventHubMDList, context) }
        }
        else {
            println("* No messages found for ${context.functionName}")
        }
    }
}

