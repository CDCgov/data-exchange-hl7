package gov.cdc.dex.reports

data class HL7JsonLakeReport (
    val configs: List<String>
) : StageContent(
    contentSchemaName = "hl7v2-json-lake-transformer",
    contentSchemaVersion = "1.0.0"
)
