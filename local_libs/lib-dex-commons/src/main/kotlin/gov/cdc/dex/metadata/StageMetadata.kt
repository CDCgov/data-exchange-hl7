package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.util.DateHelper.toIsoString
import java.util.*

data class EventGridMetaData(
    val topic: String,
    val eventTime:String
)
abstract class StageMetadata(
    @SerializedName("process_name") val processName: String,
    @SerializedName("process_version") val processVersion: String,
    //@SerializedName("eventhub_queued_time") val eventhubQueuedTime: String,
    val eventhub_queued_time: String? = " ",
    val eventhub_offset:Long? = null,
    val eventhub_sequence_number:Int? = null,
    //@SerializedName("eventhub_offset") val eventHubOffSet: Long,
    //@SerializedName("eventhub_sequence_number") val eventHubSeqNbr: Int,
    @Transient open val status: String?,
    @SerializedName("configs") val configs: List<String>,
    val eventGrid_eventTime: String? = null
) {
    @SerializedName("start_processing_time")
    var startProcessTime: String = Date().toIsoString()
    @SerializedName("end_processing_time")
    var endProcessTime: String? = null

    constructor(processName:String, processVersion:String, status: String?, eventHubMetadata: EventHubMetadata, configs: List<String> ):
            this(processName, processVersion, eventHubMetadata.EnqueuedTimeUtc, eventHubMetadata.Offset, eventHubMetadata.SequenceNumber, status, configs,null)
    constructor(processName:String, processVersion:String, status: String?, eventGridMetadata: EventGridMetaData, configs: List<String> ):
            this(processName, processVersion, null, null, null, status, configs,eventGridMetadata.eventTime)
}

