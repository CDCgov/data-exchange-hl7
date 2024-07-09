package gov.cdc.dex.reports

data class HL7JsonLakeReport (
    val configs: List<String>
) : StageContent(
    contentSchemaName = "DEX HL7v2 Json Lake",
    contentSchemaVersion = "2.0.0"
)
