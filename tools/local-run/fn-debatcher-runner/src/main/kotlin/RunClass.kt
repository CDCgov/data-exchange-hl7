import com.google.gson.GsonBuilder

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.Function
import gov.cdc.dex.hl7.context
import gov.cdc.dex.util.DateHelper.toIsoString
import java.io.File
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries

data class Url (var url:String)
data class Message(val eventType:String = "Microsoft.Storage.BlobCreated") {
    val id = UUID.randomUUID()
    lateinit var data:Url
    val eventTime = Date().toIsoString()
}

object RunClass {
    @JvmStatic
    fun main(args: Array<String>) {
        val gson = GsonBuilder().serializeNulls().create()

        val container = System.getenv("BlobIngestConnectionString")
        val containerName = System.getenv("BlobIngestContainerName")
        val eventHubMDList: MutableList<EventHubMetadata> = ArrayList()

        val messages = Path("$container/$containerName")
            .listDirectoryEntries("*.*")
            .filter {it.fileName.toString().indexOf(".properties") == -1
            }
            .mapIndexed { index, path ->
                eventHubMDList.add(EventHubMetadata(index, 99, "", ""))
                val msg = Message().apply {
                    data =  Url("/$containerName/${File(path.absolutePathString()).name}")
                }
                "[${gson.toJson(msg)}]"
            }
        if (messages.isNotEmpty() ) {
            var result = with(Function()) { eventHubProcessor(messages, eventHubMDList, context) }
            // do something with result
        }
        else {
            println("* No messages for ${context.functionName}")
        }
    }
}

