package gov.cdc.dex.reports
import com.google.gson.annotations.SerializedName

data class PSBaseReport
    (
        @SerializedName("upload_id") val uploadId: String,
        @SerializedName("user_id") val userId: String?,
        @SerializedName("data_stream_id") val dataStreamId: String,
        @SerializedName("data_stream_route") val dataStreamRoute: String,
        @SerializedName("jurisdiction") val jurisdiction: String?,
        @SerializedName("sender_id") val senderId: String,
        @SerializedName("data_producer_id") val dataProducerId: String?,
        @SerializedName("dex_ingest_timestamp") val dexIngestTimestamp: String,
        @SerializedName("message_metadata") val messageMetadata: MessageMetadata?,
        @SerializedName("stage_info") val stageInfo: StageInfo,
        @SerializedName("tags") val tags: Map<String, String>? = null,
        @SerializedName("data") val data: Map<String, String>? = null,
        @SerializedName("content") var content: StageContent?
    ) {
        @SerializedName("report_schema_version") val reportSchemaVersion: String = "1.0.0"
        @SerializedName("content_type") val contentType: String = "application/json"

    }

    data class MessageMetadata(
        @SerializedName("message_uuid") val messageUuid: String?,
        @SerializedName("message_hash") val messageHash: String?,
        @SerializedName("aggregation") val aggregation: AggregationType?,
        @SerializedName("message_index") val messageIndex: Int?
    )

    enum class AggregationType {
        SINGLE,
        BATCH
    }

    data class StageInfo(
        @SerializedName("service") val service: String = "HL7v2 Pipeline",
        @SerializedName("stage") val stage: String,
        @SerializedName("version")  val version: String,
        @SerializedName("status") val status: StageStatus,
        @SerializedName("issues")  val issues: List<Issue>?,
        @SerializedName("start_processing_time") val startProcessingTime: String?,
        @SerializedName("end_processing_time") val endProcessingTime: String?
    )

    enum class StageStatus {
       SUCCESS,
       FAILURE
    }

    data class Issue(
        @SerializedName("level") val level: IssueLevel,
        @SerializedName("message") val message: String
    )

    enum class IssueLevel {
        WARNING,
        ERROR
    }

    /** Needs to be extended by each Stage to include relevant info. **/
    abstract class StageContent(
        @SerializedName("content_schema_name") val contentSchemaName: String,
        @SerializedName("content_schema_version") val contentSchemaVersion: String
    )
