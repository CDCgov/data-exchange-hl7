package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.ProcessMetadata

import gov.cdc.dex.hl7.model.Segment 


data class LakeSegsTransProcessMetadata (override val status: String, val report: List<Segment>?) // 
    : ProcessMetadata(PROCESS_NAME, PROCESS_VERSION, status) {

        companion object  {
            const val PROCESS_NAME = "lakeSegsTransformer"
            const val PROCESS_VERSION = "1.0.0"
        } // .companion object

} // .LakeSegsTransProcessMetadata