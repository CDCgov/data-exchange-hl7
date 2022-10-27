package gov.cdc.dex.util

import com.google.gson.*
import gov.cdc.dex.metadata.ProcessMetadata

object JsonHelper {
    fun Any.toJsonElement():JsonElement {
        val jsonStr = Gson().toJson(this)
        return JsonParser.parseString(jsonStr)
    }

    fun JsonObject.addArrayElement(arrayName: String, processMD: ProcessMetadata) {
        val currentProcessPayload = this[arrayName]
        if (currentProcessPayload == null) {
            this.add(arrayName,  JsonArray())
        }
        val currentArray = this[arrayName].asJsonArray
        currentArray.add(processMD.toJsonElement())
    }
}