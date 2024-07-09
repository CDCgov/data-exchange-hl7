package gov.cdc.dataexchange.processingstatus.model

data class BaseReport(
    val report_schema_version: String,
    /** Unique upload identifier associated with this report. */
    val upload_id: String,
    /** User or system id that uploaded the file, not the provider of this report. */
    val user_id: String? = null,
    val data_stream_id: String,
    val data_stream_route: String,
    val jurisdiction: String? = null,
    val sender_id: String,
    val data_producer_id: String? = null,
    val dex_ingest_timestamp: String,
    /** Metadata associated with the message this report belong to. */
    val message_metadata: MessageMetadata? = null,
    /** Describes the stage that is providing this report. */
    val stage_info: StageInfo,
    /** Optional tag(s) associated with this report. */
    val tags: Map<String, Any>? = null,
    /** Optional data associated with this report. */
    val data: Map<String, Any>? = null,
    val content_type: String = "application/json"
    var content: Any
)

/**
 * Metadata associated with the message this report belong to.
 */
data class MessageMetadata(
    /** Unique identifier for the message associated with this report.  Null if not applicable. */
    val message_uuid: String? = null,
    /** MD5 hash of the message content. */
    val message_hash: String? = null,
    /** Enumeration: [single, batch]. */
    val aggregation: AggregationType = AggregationType.SINGLE,
    /** Index of the message; e.g. row if csv. */
    val message_index: Long = 1
)

data class StageInfo(
    /** "Name of the service associated with this report." **/
    val service: String,
    /**     Action the stage was conducting when providing this report." **/
    val stage: String,
    /**     Version of the stage providing this report" **/
    val version: String,
    /** use Status enum: ["SUCCESS", "FAILURE"] **/
    val status: Status,
    val issues: List<Issue>?,
    val start_processing_time: String?,
    val end_processing_time: String?
)

data class Issue (
    /** use IssueLevel enum string value **/
    val level: IssueLevel,
    val message: String
        )

enum class IssueLevel {
    WARNING,
    ERROR
}

