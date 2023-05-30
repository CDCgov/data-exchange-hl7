package gov.cdc.dex.hl7

import com.google.gson.JsonObject
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.ProcessMetadata

data class HL7JSONLakeProcessMetadata(override val status: String, val report: JsonObject, @Transient val eventHubMD: EventHubMetadata, val config: List<String>) //
    : ProcessMetadata(PROCESS_NAME, PROCESS_VERSION,status,eventHubMD,config) {

    companion object  {
        const val PROCESS_NAME = "HL7-JSON-LAKE-TRANSFORMER"
        const val PROCESS_VERSION = "1.0.0"
    } // .companion object

} // .LakeSegsTransProcessMetadata
