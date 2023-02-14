package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.ProcessMetadata

data class LakeSegsTransProcessMetadata (override val status: String, val report: Array<Array<String>>)
    : ProcessMetadata(PROCESS_NAME, PROCESS_VERSION, status) {

        companion object  {
            const val PROCESS_NAME = "lakeSegsTransformer"
            const val PROCESS_VERSION = "1.0.0"
        } // .companion object

} // .LakeSegsTransProcessMetadata