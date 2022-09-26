package gov.cdc.dataExchange


enum class VALIDATION_ISSUE_TYPE {DATA_TYPE, CARDINALITY, VOCAB}

data class ValidationIssue(
    val category: String,       // ERROR (for required fields) or WARNING
    val type: VALIDATION_ISSUE_TYPE,           // DATA_TYPE, CARDINALITY, VOCAB
    val fieldName: String,      // mmg field Name
    val hl7Path: String,        // HL7 path to extract value
    val lineNumber: Int,
    val errorMessage: String,   // custom error message
     )