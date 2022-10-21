package gov.cdc.dex.hl7.model

enum class ValidationIssueCategoryType(val message: String) {
    ERROR("Error"), 
    WARNING("Warning");
}

enum class ValidationIssueType(val message: String) {
    DATA_TYPE("data_type"), 
    CARDINALITY("cardinality"), 
    VOCAB("vocabulary"),
    SEGMENT_NOT_IN_MMG("segment_not_in_mmg");
}

enum class ValidationErrorMessage(val message: String) {
    DATA_TYPE_NOT_FOUND("Element data type not found"),
    DATA_TYPE_MISMATCH("Element data type is not matching the MMG data type"),
    CARDINALITY_UNDER("Element has less repeats than allowed by MMG cardinality"),
    CARDINALITY_OVER("Element has more repeats than allowed by MMG cardinality"),
    VOCAB_NOT_AVAILABLE("Vocabulary not available for code system code"),
    VOCAB_ISSUE("Vocabulary code system code and code concept not found in vocabulary entries"),
    SEGMENT_NOT_IN_MMG("HL7 segment found in the message that is part of the MMG definitions");
}


data class ValidationIssue(
    val category: ValidationIssueCategoryType,         // ERROR (for required fields) or WARNING
    val type: ValidationIssueType,                  // DATA_TYPE, CARDINALITY, VOCAB
    val fieldName: String,                          // mmg field Name
    val hl7Path: String,                            // HL7 path to extract value
    val lineNumber: Int,
    val errorMessage: ValidationErrorMessage,        // error message
    val message: String,                            // custom message to add value in question

) // .ValidationIssue