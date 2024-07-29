package gov.cdc.dex.reports
import gov.cdc.hl7.RedactInfo

data class HL7RedactorReport (
    val report: RedactorReportData?,
    val configs: List<String>?
): StageContent (
    contentSchemaName = "hl7v2-redact",
    contentSchemaVersion = "1.0.0"
)


    data class RedactorReportData (
        val entries: List<RedactInfo>
    )





