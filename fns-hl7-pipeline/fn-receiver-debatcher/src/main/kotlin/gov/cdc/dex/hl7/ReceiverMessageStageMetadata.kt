package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.EventGridStageMetadata

data class ReceiverMessageStageMetadata(
    @Transient val receiverStatus: String?,
    override val eventTimestamp: String
) :
    EventGridStageMetadata(
        stageName = ProcessInfo.RECEIVER_PROCESS,
        stageVersion = ProcessInfo.RECEIVER_VERSION,
        status = receiverStatus,
        configs = listOf(),
        eventTimestamp = eventTimestamp
    ) {

}
