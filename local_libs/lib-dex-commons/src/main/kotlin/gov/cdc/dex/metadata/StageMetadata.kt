package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.util.DateHelper.toIsoString
import java.util.*

abstract class StageMetadata(
    @Transient  open val stageName: String,
    @Transient open val stageVersion: String,
    @Transient open val status: String?,
    @Transient  open val configs: List<String>?,
    @Transient open val eventTimestamp: String
    ) {
        @SerializedName("start_processing_time")
        var startProcessTime: String = Date().toIsoString()
        @SerializedName("end_processing_time")
        var endProcessTime: String? = null
}

abstract class EventGridStageMetadata(
    @SerializedName("stage_name") override val  stageName: String,
    @SerializedName("stage_version")override val stageVersion: String,
     override val status: String?,
    @SerializedName("configs") override val configs: List<String>?,
    @SerializedName("event_timestamp")  override val eventTimestamp: String): StageMetadata(stageName, stageVersion, status, configs, eventTimestamp, ) {

}

abstract class EventHubStageMetadata(
    @SerializedName("stage_name") override val stageName: String,
    @SerializedName("stage_version") override val stageVersion: String,
     override val status: String?,
    @SerializedName("configs") override val configs: List<String>?,
    @SerializedName("event_timestamp") override val eventTimestamp: String,
    @SerializedName("eventhub_offset") val eventHubOffSet: Long,
    @SerializedName("eventhub_sequence_number") val eventHubSeqNbr: Int):StageMetadata(stageName, stageVersion, status, configs, eventTimestamp) {

        constructor(stageName:String, stageVersion:String, status: String?,  configs: List<String>,eventHubMetadata: EventHubMetadata,):
            this(stageName, stageVersion, status, configs, eventHubMetadata.EnqueuedTimeUtc, eventHubMetadata.Offset, eventHubMetadata.SequenceNumber, )

}

