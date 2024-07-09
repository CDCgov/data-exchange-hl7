package gov.cdc.dataexchange.processingstatus.model

data class HL7ReceiverReport(
    val content_schema_name: String = "DEX HL7v2 RECEIVER",
    val content_schema_version: String? = "2.0.0",
    val report: ReportData? = null
)


data class ReportData(
    var ingested_file_path: String? = null,
    var ingested_file_timestamp: String? = null,
    var ingested_file_size: Long = 0,
    var received_filename: String? = null,
    var supporting_metadata: Map<String, String>? = null,
    var aggregation: AggregationType = AggregationType.SINGLE,
    var number_of_messages: Long = 0,
    var number_of_messages_not_propagated: Long = 0,
    var error_messages: List<IngestError>? = null
)

data class IngestError(
    val message_uuid: String? = null,
    val message_index: Long = 1,
    val error_message: String? = null
)


