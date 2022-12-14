package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.ProcessMetadata

data class MbtProcessMetadata (override val status: String, val report: Map<String, Any?>)
    : ProcessMetadata(MMG_BASED_TRANSFORMER_PROCESS, MMG_BASED_TRANSFORMER_VERSION, status) {
        companion object  {
            const val MMG_BASED_TRANSFORMER_PROCESS = "MESSAGE-PROCESSOR"
            const val MMG_BASED_TRANSFORMER_VERSION = "1.0.0"
        }
} // .MbtProcessMetadata