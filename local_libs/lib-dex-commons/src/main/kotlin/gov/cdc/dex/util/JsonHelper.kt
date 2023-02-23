package gov.cdc.dex.util

import com.google.gson.*
import gov.cdc.dex.metadata.ProcessMetadata
import java.util.*

object JsonHelper {

    val gson = GsonBuilder().serializeNulls().create()
    fun Any.toJsonElement():JsonElement {
        val jsonStr = gson.toJson(this)
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
    @Throws(UnknownPropertyError::class)
    fun getValueFromJson(path: String, element: JsonElement): JsonElement {
        val paths = path.split(".")
        var e:JsonElement = element
        paths.forEach {
            try {
                e = e.asJsonObject[it]
            } catch (e: NullPointerException) {
                throw UnknownPropertyError("Property $it not recognized in json")
            }
        }
        return e
    }
    fun getValueFromJsonAndBase64Decode(path: String, element: JsonElement): String {
        val encodedValue= getValueFromJson(path,element).asString
        return String(Base64.getDecoder().decode(encodedValue))
    }

    fun getStringArrayFromJsonArray(array: JsonArray) : Array<String> {
        val newStringArray = arrayListOf<String>()
        array.forEach {
            newStringArray.add(it.asString.trim())
        }
        return newStringArray.toTypedArray()
    }
}