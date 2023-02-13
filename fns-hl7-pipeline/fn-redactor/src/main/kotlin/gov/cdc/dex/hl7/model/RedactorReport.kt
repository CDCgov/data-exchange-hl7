package gov.cdc.dex.hl7.model

import gov.cdc.hl7.RedactInfo
import scala.collection.immutable.List

data class RedactorReport(val entries: List<RedactInfo>){
    val status = "REDACTOR-WARNINGS"

}


