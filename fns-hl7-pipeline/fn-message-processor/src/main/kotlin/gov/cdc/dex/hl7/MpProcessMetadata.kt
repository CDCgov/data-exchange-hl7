package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.ProcessMetadata

data class MpProcessMetadata (override val status: String, val report: Map<String, Any?>)
    : ProcessMetadata(MESSAGE_PROCESSOR_PROCESS, MESSAGE_PROCESSOR_VERSION, status) {
        companion object  {
            const val MESSAGE_PROCESSOR_PROCESS = "MESSAGE-PROCESSOR"
            const val MESSAGE_PROCESSOR_VERSION = "1.0.0"
        }
} // .MpProcessMetadata