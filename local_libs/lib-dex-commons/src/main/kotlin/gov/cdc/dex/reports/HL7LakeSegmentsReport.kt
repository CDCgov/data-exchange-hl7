package gov.cdc.dex.reports

data class HL7LakeSegmentsReport (
    val configs: List<String>
        ) : StageContent(
    contentSchemaName = "DEX HL7v2 Lake of Segments",
    contentSchemaVersion = "2.0.0"
        )
