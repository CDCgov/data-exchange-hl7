package local

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.io.path.Path
import kotlin.io.path.writeText

fun writeToHub( evHubConnStr:String, evHubName:String, messages:List<String>) {
    messages.forEach {
        // parse the message, to extract file properties
        // from provenance
        val provenance = (JsonParser.parseString(it) as JsonObject)
            .getAsJsonObject("metadata")
            .getAsJsonObject("provenance")
        val msgIndex = provenance.get("message_index").asString
        val filePath = provenance.get("ext_original_file_name").asString
        val fileNameAndExt = parseFileName(filePath)

        val fullFileName =
            "$evHubConnStr/$evHubName/${fileNameAndExt.first}-$msgIndex${fileNameAndExt.second}"
        Path(fullFileName).writeText(it)
    }
}

fun writeToContainer( containerPath:String, messages:List<JsonObject>) {
    messages.forEach {
        Path("$containerPath/${it.get("id").asString.replace("\"", "")}").writeText(it.toString())
    }
}

fun parseFileName(filePath:String ): Pair<String,String> {
    val fileNameWithExt = filePath.substringAfterLast("/")
    val extIndex = fileNameWithExt.lastIndexOf('.')
    return Pair(fileNameWithExt.substring(0, extIndex), fileNameWithExt.substring(extIndex))
}
