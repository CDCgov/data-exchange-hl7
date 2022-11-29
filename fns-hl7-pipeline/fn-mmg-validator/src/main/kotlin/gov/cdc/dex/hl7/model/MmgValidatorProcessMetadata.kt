package gov.cdc.dex.hl7.model

import gov.cdc.dex.metadata.ProcessMetadata

data class MmgValidatorProcessMetadata (override val status: String, val report: ValidationReport): ProcessMetadata(
    MMG_VALIDATOR_PROCESS, MMG_VALIDATOR_VERSION, status) {
    companion object  {
        const val MMG_VALIDATOR_PROCESS = "MMG-VALIDATOR"
        const val MMG_VALIDATOR_VERSION = "1.0.0"
    }
}