package gov.cdc.dataexchange

import com.google.gson.JsonObject
import com.microsoft.azure.functions.HttpRequestMessage
import org.slf4j.LoggerFactory

class Helper {

    companion object {
        private val logger = LoggerFactory.getLogger(Helper::class.java.simpleName)

        // change database and container
        fun checkRequest(request: HttpRequestMessage<*>?): Boolean {
            if(request != null) {
                val recordId = request.headers["id"]
                val partitionKey = request.headers["partition-key"]
                if (recordId == null || partitionKey == null) {
                    return false
                }
            } else { return false }
            return true
        }

        // Update event timestamp of a given JsonObject.
        fun updateEventTimestamp(inputEvent: JsonObject, newTimestamp: String): JsonObject {
            logger.info("Updating timestamp to $newTimestamp")
            val metadata = inputEvent.getAsJsonObject("metadata")
            val provenance = metadata.getAsJsonObject("provenance")
            provenance.addProperty("event_timestamp", newTimestamp)
            return inputEvent
        }
    }
}