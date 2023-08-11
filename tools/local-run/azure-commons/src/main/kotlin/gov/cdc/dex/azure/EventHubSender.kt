package gov.cdc.dex.azure

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.slf4j.LoggerFactory
import kotlin.io.path.Path
import kotlin.io.path.writeText
import util.*

class EventHubSender (val evHubConnStr: String, ) {
    companion object {
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    }
    fun send(evHubTopicName: String, message: String ) {
        send(evHubTopicName, listOf(message))
    }
    fun send(evHubTopicName: String, messages:List<String>) {
        messages.forEach{
            // parse the message, to extract file properties
            // from provenance
            val provenance = (JsonParser.parseString(it) as JsonObject)
                .getAsJsonObject("metadata")
                .getAsJsonObject("provenance")
            val msgIndex = provenance.get("message_index").getAsString()
            val filePath = provenance.get("ext_original_file_name").getAsString()
            val fileNameAndExt = parseFileName(filePath)

            var fullFileName = "$evHubConnStr/$evHubTopicName/${fileNameAndExt.first}-$msgIndex${fileNameAndExt.second}"
            Path(fullFileName).writeText(it)
        }
    }
}