package gov.cdc.dex.hl7.model

enum class ReportStatus(val message: String) {
    MMG_VALID("MMG-VALID"),
    MMG_ERRORS("MMG-ERRORS"), 
    MMG_WARNINGS("MMG-WARNINGS");
}
