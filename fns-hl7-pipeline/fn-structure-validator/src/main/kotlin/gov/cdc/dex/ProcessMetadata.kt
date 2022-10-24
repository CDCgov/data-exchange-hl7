package gov.cdc.dex

import com.google.gson.annotations.SerializedName
import gov.cdc.nist.validator.NistReport
import java.text.SimpleDateFormat
import java.util.*

data class ProcessMetadata(
    @SerializedName("process_name") val processName: String,
    @SerializedName("process_version") val processVersion: String,
    @SerializedName("event_id") open val eventId: String,
    @SerializedName("event_timestamp") open val eventTimestamp: String,
    @SerializedName("status") open val status: String
) {
    @SerializedName("start_processing_time")
    var startProcessTime: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format( Calendar.getInstance().time)
    @SerializedName("end_processing_time")
    var endProcessTime: String? = null
    var report: NistReport? = null
}
