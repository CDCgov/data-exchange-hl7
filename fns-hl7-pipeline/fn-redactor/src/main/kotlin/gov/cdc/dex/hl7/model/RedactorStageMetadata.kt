package gov.cdc.dex.hl7.model

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.EventHubStageMetadata


data class RedactorStageMetadata(
   @Transient val redactorStatus: String,
    val report: RedactorReport,
    @Transient val eventHubMetadata: EventHubMetadata,
    @Transient val config: List<String>
) : EventHubStageMetadata(
    stageName = REDACTOR_PROCESS,
    stageVersion = REDACTOR_VERSION,
    status = redactorStatus,
    eventHubMetadata = eventHubMetadata,
    configs = config
) {
    companion object {
        const val REDACTOR_PROCESS = "REDACTOR"
        const val REDACTOR_VERSION = "1.0.0"
    }


}