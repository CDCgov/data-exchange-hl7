package gov.cdc.dex.reports

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class HL7ReceiverReport(val report: ReceiverReportData) : StageContent (
    contentSchemaName = "hl7v2-redact.1.0.0.schema.json",
    contentSchemaVersion = "2.0.0",
)

data class ReceiverReportData(
    @SerializedName("ingested_file_path") var ingestedFilePath: String? = null,
    @SerializedName("ingested_file_timestamp") var ingestedFileTimestamp: String? = null,
    @SerializedName("ingested_file_size") var ingestedFileSize: Long = 0,
    @SerializedName("received_filename") var receivedFileName: String? = null,
    @SerializedName("supporting_metadata")  var supportingMetadata: Map<String, String>? = null,
    @SerializedName("aggregation") var aggregation: AggregationType = AggregationType.SINGLE,
    @SerializedName("number_of_messages") var numberOfMessages: Long = 0,
    @SerializedName("number_of_messages_not_propagated")var numberOfMessagesNotPropagated: Long = 0,
    @SerializedName("error_messages") var errorMessages: List<IngestError>? = null
)

data class IngestError(
   @SerializedName("message_uuid") val messageUuid: String? = null,
   @SerializedName("message_index") val messageIndex: Long = 1,
   @SerializedName("error_message") val errorMessage: String? = null
)


