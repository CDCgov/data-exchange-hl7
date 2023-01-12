package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.ProcessMetadata

data class MbtProcessMetadata (
                            override val status: String,
                            val report: Any?
                            )
    : ProcessMetadata(PROCESS_NAME, PROCESS_VERSION, status) {
        companion object {
            val PROCESS_NAME = "mmgBasedTransformer"
            val PROCESS_VERSION = "1.0.0"
        }
} // .MbtProcessMetadata


// report for happy path
// /*Map<String, Any?>*/