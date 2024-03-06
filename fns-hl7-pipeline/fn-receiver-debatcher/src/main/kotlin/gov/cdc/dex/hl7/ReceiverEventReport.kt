package gov.cdc.dex.hl7
import com.google.gson.annotations.SerializedName
import gov.cdc.dex.metadata.RoutingMetadata

data class ReceiverEventReport(
    @SerializedName("single_or_batch") var messageBatch : String = "SINGLE",
    @SerializedName("number_of_messages") var totalMessageCount: Int = 0,
    @SerializedName("number_of_messages_not_propagated") var notPropogatedCount: Int = 0,
    @SerializedName("error_messages") val errorMessages: MutableList<ReceiverEventError> = mutableListOf()
    )

data class ReceiverEventError (
    @SerializedName("message_index") val messageIndex: Int,
    @SerializedName("message_uuid") val messageUUID: String?,
    @SerializedName("error_message") val error: String?
        )

data class ReceiverEventStageMetadata (
   @SerializedName("stage_name")  val stageName : String = ProcessInfo.RECEIVER_PROCESS,
   @SerializedName("stage_version")  val stageVersion : String = ProcessInfo.RECEIVER_VERSION,
   @SerializedName("event_timestamp") var eventTimestamp: String? = null,
   @SerializedName("start_processing_time") val startProcessingTime : String,
   @SerializedName("end_processing_time") var endProcessingTime: String? = null,
    var report : ReceiverEventReport? = null
        )

data class ReceiverEventMetadata (
    @SerializedName("schema_name") val schemaName: String = "DEX HL7v2 RECEIVER",
    @SerializedName("schema_version") val schemaVersion: String = "2.0.0",
    @SerializedName("routing_metadata") var routingData: RoutingMetadata? = null,
    var stage : ReceiverEventStageMetadata
        )