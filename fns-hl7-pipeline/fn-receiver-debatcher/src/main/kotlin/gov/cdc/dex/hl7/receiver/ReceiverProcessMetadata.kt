package gov.cdc.dex.hl7.receiver

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.ProcessMetadata

data class ReceiverProcessMetadata (override val status: String?,  @Transient val eventHubMD: EventHubMetadata): ProcessMetadata(RECEIVER_PROCESS, RECEIVER_VERSION , status, eventHubMD, listOf()) {
    companion object  {
        const val RECEIVER_PROCESS = "RECEIVER"
        const val RECEIVER_VERSION = "1.0.0"
    }
}