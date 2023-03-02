package gov.cdc.dex.hl7.model

import gov.cdc.hl7.RedactInfo

data class RedactorReport(val entries: List<RedactInfo>) {
    val status = "SUCCESS"

}