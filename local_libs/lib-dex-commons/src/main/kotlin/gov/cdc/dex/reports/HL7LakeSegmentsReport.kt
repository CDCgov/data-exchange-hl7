package gov.cdc.dex.reports

data class HL7LakeSegmentsReport (
    val configs: List<String>
        ) : StageContent(
    contentSchemaName = "hl7v2-lake-segments-transformer.1.0.0.schema.json",
    contentSchemaVersion = "2.0.0"
        )
