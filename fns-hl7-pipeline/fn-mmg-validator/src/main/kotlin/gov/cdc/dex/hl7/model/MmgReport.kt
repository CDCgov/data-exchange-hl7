package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName

data class MmgReport( private val entries: List<ValidationIssue>) {
    @SerializedName("error-count") val errorCount = entries.count { it.classification == ValidationIssueCategoryType.ERROR}
    @SerializedName("warning-count")val warningCount = entries.count{ it.classification == ValidationIssueCategoryType.WARNING}
    val status = if (errorCount == 0) ReportStatus.MMG_VALID else ReportStatus.MMG_ERRORS

} // .MmgReporter

enum class ReportStatus(val message: String) {
    MMG_VALID("MMG-VALID"),
    MMG_ERRORS("MMG-ERRORS"),
    MMG_WARNINGS("MMG-WARNINGS"); // not used atm
}