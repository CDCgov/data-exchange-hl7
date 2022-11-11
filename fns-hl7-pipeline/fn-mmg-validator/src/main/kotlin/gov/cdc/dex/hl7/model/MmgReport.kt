package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName

data class MmgReport(@SerializedName("error-count") private val errorCount:Int,
                @SerializedName("warning-count") private val warningCount: Int,
                private val entries: List<ValidationIssue>) {
    val status = if (errorCount == 0) ReportStatus.MMG_VALID else ReportStatus.MMG_ERRORS

} // .MmgReporter

enum class ReportStatus(val message: String) {
    MMG_VALID("MMG-VALID"),
    MMG_ERRORS("MMG-ERRORS"),
    MMG_WARNINGS("MMG-WARNINGS"); // not used atm
}