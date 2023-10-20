package gov.cdc.dataexchange.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class ServiceUtil {
    companion object {

        fun mapMessages(records: List<String>): List<Map<String, Any>> {
            return records.map { record ->
                    convertJsonToMap(record)
                }
        }

        private fun convertJsonToMap(json: String): Map<String, Any> {
            val gson = Gson()
            val mapType: Type = object : TypeToken<Map<String, Any>>() {}.type
            return gson.fromJson(json, mapType)
        }
    }
}