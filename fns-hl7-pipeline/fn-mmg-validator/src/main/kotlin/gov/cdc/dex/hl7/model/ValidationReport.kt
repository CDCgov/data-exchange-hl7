package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName

open class ValidationReport(open val entries: List<ValidationIssue>?) {

    companion object {
        const val VALID_MESSAGE = "VALID-MESSAGE"
        const val INVALID_MESSAGE = "INVALID-MESSAGE"
    }

    @SerializedName("error-count")
    val errorCount = entries?.count { it.classification == ValidationIssueCategoryType.ERROR }
    @SerializedName("warning-count")
    val warningCount = entries?.count { it.classification == ValidationIssueCategoryType.WARNING }
    val status = if (errorCount == 0) VALID_MESSAGE else INVALID_MESSAGE
}