package gov.cdc.dex.hl7

import com.google.gson.JsonObject
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.EventHubStageMetadata

data class HL7JSONLakeStageMetadata(
    @Transient val jsonLakeStatus: String,
    val output: JsonObject?,
    @Transient val eventHubMD: EventHubMetadata,
    @Transient val config: List<String>
) : EventHubStageMetadata(
    stageName = PROCESS_NAME,
    stageVersion = PROCESS_VERSION,
    status = jsonLakeStatus, eventHubMetadata = eventHubMD, configs = config
) {

    companion object {
        const val PROCESS_NAME = "HL7-JSON-LAKE-TRANSFORMER"
        const val PROCESS_VERSION = "1.0.0"
    }

}
