package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.EventGridStageMetadata

data class ReceiverProcessMetadata (override val status: String?,
                               override val eventTimestamp: String): EventGridStageMetadata (
    RECEIVER_PROCESS, RECEIVER_VERSION , status, listOf(), eventTimestamp ) {
    companion object  {
        const val RECEIVER_PROCESS = "RECEIVER"
        const val RECEIVER_VERSION = "1.0.0"
    }
}
