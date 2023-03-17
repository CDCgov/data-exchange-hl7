package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import gov.cdc.dex.azure.EventHubMetadata
import java.text.SimpleDateFormat
import java.util.*

abstract class ProcessMetadata(
    @SerializedName("process_name") val processName: String,
    @SerializedName("process_version") val processVersion: String,
    @SerializedName("eventhub_queued_time") val eventhubQueuedTime: String,
    @SerializedName("eventhub_offset") val eventHubOffSet: Long,
    @SerializedName("eventhub_sequence_number") val eventHubSeqNbr: Int,
    @Transient open val status: String?,
    @SerializedName("configs") val configs: List<String>
) {
    @SerializedName("start_processing_time")
    var startProcessTime: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format( Calendar.getInstance().time)
    @SerializedName("end_processing_time")
    var endProcessTime: String? = null

    constructor(processName:String, processVersion:String, status: String?, eventHubMetadata: EventHubMetadata, configs: List<String> ):
            this(processName, processVersion, eventHubMetadata.EnqueuedTimeUtc, eventHubMetadata.Offset, eventHubMetadata.SequenceNumber, status, configs)
}
