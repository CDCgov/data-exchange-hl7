package gov.cdc.dex.hl7

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.ProcessMetadata

data class MmgSqlTransProcessMetadata (
    override val status: String,
    val report: Any?,
    @Transient val eventHubMD: EventHubMetadata /*Map<String, Any?>*/)
    : ProcessMetadata(PROCESS_NAME, PROCESS_VERSION, status, eventHubMD) {
        companion object  {
            const val PROCESS_NAME = "mmgSQLTransformer"
            const val PROCESS_VERSION = "1.0.0"
        } // .companion object

} // .MmgSqlTransProcessMetadata