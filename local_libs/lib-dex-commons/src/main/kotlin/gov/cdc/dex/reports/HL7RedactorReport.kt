package gov.cdc.dex.reports

data class Hl7RedactorReport (
    val report: RedactorReportData?,
    val configs: List<String>?
): StageContent (
    contentSchemaName = "DEX HL7v2 REDACTOR",
    contentSchemaVersion = "2.0.0"
)


    data class RedactorReportData (
        val entries: List<RedactionEntry>? = null
    )

    data class RedactionEntry (
        val path: String,
        val rule: String,
        val lineNumber: Long,
        val fieldIndex: Long? = null
    )




