package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.ProcessMetadata

import gov.cdc.dex.hl7.model.ReportStatus
import gov.cdc.dex.hl7.model.ValidationIssue

data class MmgValidatorProcessMetadata (override val status: String, val report: List<ValidationIssue>?): ProcessMetadata(MMG_VALIDATOR_PROCESS, MMG_VALIDATOR_VERSION, status) {
    companion object  {
        const val MMG_VALIDATOR_PROCESS = "MMG-VALIDATOR"
        const val MMG_VALIDATOR_VERSION = "1.0.0"
    }
}