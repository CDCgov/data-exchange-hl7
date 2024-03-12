package gov.cdc.dex.hl7

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.model.Segment
import gov.cdc.dex.metadata.EventHubStageMetadata

data class LakeSegsTransStageMetadata(
    @Transient val lakeSegStatus: String,
    val output: List<Segment>?,
    @Transient val eventHubMD: EventHubMetadata,
    @Transient val config: List<String>
) //
    : EventHubStageMetadata(
    stageName = PROCESS_NAME,
    stageVersion = PROCESS_VERSION,
    status = lakeSegStatus,
    eventHubMetadata = eventHubMD,
    configs = config
) {

    companion object {
        const val PROCESS_NAME = "LAKE-SEGMENTS-TRANSFORMER"
        val PROCESS_VERSION = System.getenv("FN_VERSION")?.toString() ?: "Unknown"
    } // .companion object

}
