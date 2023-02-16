package gov.cdc.dex.hl7.model

import gov.cdc.dex.metadata.ProcessMetadata
import gov.cdc.hl7.RedactInfo


data class RedactorProcessMetadata(override val status: String, val report: List<RedactInfo>?): ProcessMetadata(
    REDACTOR_PROCESS, REDACTOR_VERSION, status) {
    companion object  {
        const val REDACTOR_PROCESS = "REDACTOR"
        const val REDACTOR_VERSION = "1.0.0"
}


}