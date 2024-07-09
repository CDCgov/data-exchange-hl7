package gov.cdc.dex.reports
import gov.cdc.nist.validator.NistReport

data class HL7StructureValidatorReport (
    val report: NistReport?,
    val configs: List<String>?
) : StageContent (
    contentSchemaName = "DEX HL7v2 Validation Report",
    contentSchemaVersion = "2.0.0"
        )
