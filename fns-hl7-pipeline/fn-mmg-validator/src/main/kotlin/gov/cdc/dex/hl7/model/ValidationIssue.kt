package gov.cdc.dex.hl7.model

enum class ValidationIssueCategoryType(val message: String) {
    ERROR("Error"), 
    WARNING("Warning");
}

enum class ValidationIssueType(val message: String) {
    DATA_TYPE("data_type"), 
    CARDINALITY("cardinality"), 
    VOCAB("vocabulary"),
    SEGMENT_NOT_IN_MMG("segment_not_in_mmg"),
    OBSERVATION_SUB_ID_VIOLATION("observation_sub_id_violation");
}

enum class ValidationErrorMessage(val message: String) {
    DATA_TYPE_NOT_FOUND("Element data type not found"),
    DATA_TYPE_MISMATCH("Element data type is not matching the MMG data type"),
    CARDINALITY_UNDER("Element has less repeats than allowed by MMG cardinality"),
    CARDINALITY_OVER("Element has more repeats than allowed by MMG cardinality"),
    VOCAB_NOT_AVAILABLE("Vocabulary not available for code system code"),
    VOCAB_ISSUE("Vocabulary code system code and code concept not found in vocabulary entries"),
    SEGMENT_NOT_IN_MMG("HL7 segment found in the message that is part of the MMG definitions"),
    OBSERVATION_SUB_ID_MISSING("Observation Sub-Id must be populated for data elements in a repeating group."),
    OBSERVATION_SUB_ID_NOT_UNIQUE("The combination of the data element identifier (OBX-3) and the observation sub-id (OBX-4) must be unique.")
}


data class ValidationIssue(
    val classification: ValidationIssueCategoryType,         // ERROR (for required fields) or WARNING
    val category: ValidationIssueType,                  // DATA_TYPE, CARDINALITY, VOCAB
    val fieldName: String,                          // mmg field Name
    val path: String,                            // HL7 path to extract value
    val line: Int,
    val errorMessage: ValidationErrorMessage,        // error message
    val description: String,                            // custom message to add value in question

) // .ValidationIssue

