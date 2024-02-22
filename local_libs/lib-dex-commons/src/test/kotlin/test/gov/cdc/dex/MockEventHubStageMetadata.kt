package test.gov.cdc.dex

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.EventHubStageMetadata

class MockEventHubStageMetadata(
    stageName: String,
    stageVersion: String,
    status: String,
    config: List<String>,
    eventHubMD: EventHubMetadata
) : EventHubStageMetadata(stageName, stageVersion, status, config, eventHubMD) {

}
