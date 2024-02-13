package gov.cdc.dex.hl7

import com.google.gson.annotations.SerializedName
import gov.cdc.dex.azure.EventHubMetadata
import java.text.SimpleDateFormat
import java.util.*

data class EventGridMetadata(
   // var SequenceNumber:Int,
    var Offset:Long,
   // var PartitionKey:String?,
   // var EnqueuedTimeUtc:String
)


    abstract class ProcessMetadata2(
        @SerializedName("process_name") val processName: String,
        @SerializedName("process_version") val processVersion: String,
        //@SerializedName("eventGrid_queued_time") val eventGridQueuedTime: String,
        //@SerializedName("eventGrid_offset") val eventGridOffSet: Long,
       // @SerializedName("eventGrid_sequence_number") val eventGridSeqNbr: Int,
        @Transient open val status: String?,
        @SerializedName("configs") val configs: List<String>
    ) {
        @SerializedName("start_processing_time")
        var startProcessTime: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format( Calendar.getInstance().time)
        @SerializedName("end_processing_time")
        var endProcessTime: String? = null

        constructor(processName:String, processVersion:String, status: String?, eventGridMetadata: String, configs: List<String> ):
                this(processName, processVersion,  status, configs)
    }
