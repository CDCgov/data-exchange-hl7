package gov.cdc.dataexchange.model

data class Hl7ReceiverReport(
    val content_schema_name: String = "DEX HL7v2 RECEIVER",
    val content_schema_version: String? = "2.0.0",
    val report: Report? = null
)


data class Report(
    var ingested_file_path: String? = null,
    var ingested_file_timestamp: String? = null,
    var ingested_file_size: Long? = null,
    var received_filename: String? = null,
    var supporting_metadata: Map<String, Any?>? = null,
    var aggregation: String = AggregationType.SINGLE.name,
    var number_of_messages: Long? = null,
    var number_of_messages_not_propagated: Long? = null,
    var error_messages: List<IngestError>? = null
)

data class IngestError(
    val message_uuid: String? = null,
    val message_index: Long? = null,
    val error_message: String? = null
)


