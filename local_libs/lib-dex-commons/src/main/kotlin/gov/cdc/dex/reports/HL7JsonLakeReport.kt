package gov.cdc.dex.reports

data class HL7JsonLakeReport (
    val configs: List<String>
) : StageContent(
    contentSchemaName = "hl7v2-json-lake-transformer.1.0.0.schema.json",
    contentSchemaVersion = "2.0.0"
)
