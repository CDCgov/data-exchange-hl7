package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.EventGridStageMetadata

data class ReceiverStageMetadata(
    @Transient val receiverStatus: String?,
    override val eventTimestamp: String
) :
    EventGridStageMetadata(
        stageName = RECEIVER_PROCESS,
        stageVersion = RECEIVER_VERSION,
        status = receiverStatus,
        configs = listOf(),
        eventTimestamp = eventTimestamp
    ) {
    companion object {
        const val RECEIVER_PROCESS = "RECEIVER"
        const val RECEIVER_VERSION = "2.0.0"
    }
}
