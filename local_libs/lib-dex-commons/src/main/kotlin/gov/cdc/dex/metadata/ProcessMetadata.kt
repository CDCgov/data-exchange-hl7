package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

abstract class ProcessMetadata(
    @SerializedName("process_name") val processName: String,
    @SerializedName("process_version") val processVersion: String,
    @Transient open val status: String?
) {
    @SerializedName("start_processing_time")
    var startProcessTime: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format( Calendar.getInstance().time)
    @SerializedName("end_processing_time")
    var endProcessTime: String? = null
}
