package gov.cdc.dex.reports
import com.google.gson.JsonObject

data class HL7StructureValidatorReport (
    val report: JsonObject,
    val configs: List<String>?
) : StageContent (
    contentSchemaName = "hl7v2-structure-validation",
    contentSchemaVersion = "1.0.0"
        )
