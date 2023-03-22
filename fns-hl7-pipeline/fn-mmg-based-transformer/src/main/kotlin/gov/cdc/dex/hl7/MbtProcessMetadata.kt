package gov.cdc.dex.hl7

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.ProcessMetadata

data class MbtProcessMetadata (
    override val status: String,
    val report: Any?,
    @Transient val eventHubMD: EventHubMetadata,
    val config : List<String>
)
    : ProcessMetadata(PROCESS_NAME, PROCESS_VERSION, status, eventHubMD, config) {
        companion object {
            const val PROCESS_NAME = "mmgBasedTransformer"
            const val PROCESS_VERSION = "1.0.0"
        }
} // .MbtProcessMetadata


// report for happy path
// /*Map<String, Any?>*/