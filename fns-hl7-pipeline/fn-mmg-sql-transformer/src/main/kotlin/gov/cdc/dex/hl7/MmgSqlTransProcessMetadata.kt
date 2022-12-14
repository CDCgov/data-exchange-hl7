package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.ProcessMetadata

data class MmgSqlTransProcessMetadata (override val status: String, val report: Any? /*Map<String, Any?>*/)
    : ProcessMetadata(MMG_SQL_TRANSFORMER_PROCESS, MMG_SQL_TRANSFORMER_VERSION, status) {
        companion object  {
            const val MMG_SQL_TRANSFORMER_PROCESS = "MMG-SQL-TRANSFORMER"
            const val MMG_SQL_TRANSFORMER_VERSION = "1.0.0"
        }
} // .MmgSqlTransProcessMetadata