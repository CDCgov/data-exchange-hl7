package test.gov.cdc.dex

import gov.cdc.dex.metadata.EventGridStageMetadata

class MockEventGridStageMetadata(
    stageName: String,
    stageVersion: String,
    status: String,
    config: List<String>,
    timestamp: String
) : EventGridStageMetadata(stageName, stageVersion, status, config, timestamp) {

}
