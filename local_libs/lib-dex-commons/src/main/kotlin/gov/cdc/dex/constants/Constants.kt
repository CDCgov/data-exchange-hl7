package gov.cdc.dex.constants


object Constants {
    const val UTF_BOM = "\uFEFF"
    const val STATUS_SUCCESS = "SUCCESS"
    const val STATUS_ERROR = "ERROR"
    const val STATUS_FAILURE = "FAILURE"
    const val STATUS_NO_VALID_MESSAGE = "No valid message found."
    const val STATUS_RECEIVED = "RECEIVED"

    val metadataObject:Set<String> = setOf(
        "message_type",
        "route",
        "reporting_jurisdiction",
        "original_file_name",
        "original_file_timestamp",
        "system_provider",
    )

    const val PROCESSES = "processes"
    const val MESSAGE_TYPE_ELR = "ELR"
    const val MESSAGE_TYPE_CASE = "CASE"
    const val MESSAGE_SUMMARY = "summary"
    const val NO_VALID_MESSAGE_ERROR = "No valid message found."
    const val MESSAGE_MISSING_REQUIRED_METADATA = "Message missing required Meta Data."

    const val SEGMENT_MSH = "MSH"

    const val PAYLOAD_ATTRIBUTE_NAME_CONTENT = "content"
    const val PAYLOAD_ATTRIBUTE_NAME_METADATA = "metadata"

    const val PAYLOAD_METADATA_ATTRIBUTE_FILEPATH = "metadata.provenance.file_path"

    const val MESSAGE_IDENTIFIER = "message_uuid"
    const val PAYLOAD_MESSAGE_INFO_ATTRIBUTE_TYPE = "message_info.type"
    const val PAYLOAD_MESSAGE_INFO_ATTRIBUTE_ROUTE = "message_info.route"

    const val DEX_CMD_RECEIVER_DEBATCHER = "DEX::RECEIVER_DEBATCHER"
    const val DEX_CMD_REDACTOR = "DEX::REDACTOR"
    const val DEX_CMD_STRUCTURE_VALIDATOR = "DEX::STRUCTURE_VALIDATOR"
    const val DEX_CMD_JSON_LAKE = "DEX::JSON_LAKE"
    const val DEX_CMD_LAKE_OF_SEGMENTS = "DEX::LAKE_OF_SEGMENTS"


}