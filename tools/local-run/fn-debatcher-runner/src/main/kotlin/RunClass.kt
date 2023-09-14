import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.Function
import gov.cdc.dex.hl7.OutBinding
import gov.cdc.dex.util.DateHelper.toIsoString
import java.io.File
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries
import local.*

data class Url (var url:String)
data class Message(val eventType:String = "Microsoft.Storage.BlobCreated") {
    val id = UUID.randomUUID()
    lateinit var data:Url
    val eventTime = Date().toIsoString()
}

object RunClass {
    val evHubOkName: String = System.getenv("EventHubSendOkName")
    val evHubErrorName: String = System.getenv("EventHubSendErrsName")
    val evHubConnStr = System.getenv("EventHubConnectionString")
    val containerPath = System.getenv("ContainerPath")

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
            val ok = OutBinding<List<String>>()
            val err = OutBinding<List<String>>()
            val cosmos = OutBinding<List<JsonObject>>()
            with(Function()) {
                eventHubProcessor(messages, eventHubMDList, ok, err, cosmos)
            }
            ok.value?.let { writeToHub(evHubConnStr, evHubOkName, it) }
            err.value?.let { writeToHub(evHubConnStr, evHubErrorName, it) }
            cosmos.value?.let {writeToContainer(containerPath, it) }
        }
    }
}

