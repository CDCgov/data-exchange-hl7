package gov.cdc.dex.hl7

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.StageMetadata

data class ReceiverProcessMetadata (override val status: String?,
                                    @Transient val eventHubMD: EventHubMetadata): StageMetadata(
    RECEIVER_PROCESS, RECEIVER_VERSION , status, eventHubMD, listOf()) {
    companion object  {
        const val RECEIVER_PROCESS = "RECEIVER"
        const val RECEIVER_VERSION = "1.0.0"
    }
}