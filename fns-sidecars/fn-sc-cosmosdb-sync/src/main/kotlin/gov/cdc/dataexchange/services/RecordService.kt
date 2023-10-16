package gov.cdc.dataexchange.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import reactor.core.publisher.Flux
import java.lang.reflect.Type

class RecordService {
    companion object {

        fun fluxRecords(records: List<String>): Flux<Map<String, Any>> {
            return Flux.fromIterable(records)
                .map { record ->
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