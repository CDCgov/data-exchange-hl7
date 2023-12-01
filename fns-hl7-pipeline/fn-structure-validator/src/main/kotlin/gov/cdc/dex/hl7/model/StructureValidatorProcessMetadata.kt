package gov.cdc.dex.hl7.model

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.ProcessMetadata
import gov.cdc.nist.validator.NistReport

data class StructureValidatorProcessMetadata (override val status: String, val report: NistReport?, @Transient val eventHubMD: EventHubMetadata, @Transient val config : List<String>): ProcessMetadata(
    VALIDATOR_PROCESS, VALIDATOR_VERSION, status, eventHubMD, config) {
    companion object  {
        const val VALIDATOR_PROCESS = "STRUCTURE-VALIDATOR"
        const val VALIDATOR_VERSION = "1.0.0"
    }

}