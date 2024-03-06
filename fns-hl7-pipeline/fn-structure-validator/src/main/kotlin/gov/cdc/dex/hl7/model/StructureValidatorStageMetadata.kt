package gov.cdc.dex.hl7.model

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.EventHubStageMetadata
import gov.cdc.nist.validator.NistReport

data class StructureValidatorStageMetadata(
    @Transient val structStatus: String,
    val report: NistReport?,
    @Transient val eventHubMD: EventHubMetadata,
    @Transient val config: List<String>
) : EventHubStageMetadata(
    stageName = VALIDATOR_PROCESS,
    stageVersion = VALIDATOR_VERSION,
    status = structStatus,
    eventHubMetadata = eventHubMD,
    configs = config
) {
    companion object {
        const val VALIDATOR_PROCESS = "STRUCTURE-VALIDATOR"
        val VALIDATOR_VERSION = System.getenv("FN_VERSION")?.toString() ?: "Unknown"
    }

}