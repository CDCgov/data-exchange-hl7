package gov.cdc.dex.hl7

import com.google.gson.JsonObject
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.EventHubStageMetadata
import gov.cdc.dex.metadata.StageMetadata

data class HL7JSONLakeProcessMetadata(override val status: String, val output: JsonObject?, @Transient val eventHubMD: EventHubMetadata, @Transient val config: List<String>) //
    : EventHubStageMetadata(PROCESS_NAME, PROCESS_VERSION,status, eventHubMetadata = eventHubMD, configs=config) {

    companion object  {
        const val PROCESS_NAME = "HL7-JSON-LAKE-TRANSFORMER"
        const val PROCESS_VERSION = "1.0.0"
    } // .companion object

}
