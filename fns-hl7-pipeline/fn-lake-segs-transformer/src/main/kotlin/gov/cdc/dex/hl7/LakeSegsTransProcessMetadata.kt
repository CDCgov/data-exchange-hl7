package gov.cdc.dex.hl7

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.ProcessMetadata

import gov.cdc.dex.hl7.model.Segment

data class LakeSegsTransProcessMetadata (override val status: String,  val report: List<Segment>?,@Transient val eventHubMD: EventHubMetadata, @Transient val config : List<String>) //
    : ProcessMetadata(PROCESS_NAME, PROCESS_VERSION,status,eventHubMD,config) {

        companion object  {
            const val PROCESS_NAME = "LAKE-SEGMENTS-TRANSFORMER"
            const val PROCESS_VERSION = "1.0.0"
        } // .companion object

} // .LakeSegsTransProcessMetadata
