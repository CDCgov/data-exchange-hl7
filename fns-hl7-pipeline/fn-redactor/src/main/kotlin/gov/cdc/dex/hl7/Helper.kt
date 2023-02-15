package gov.cdc.dex.hl7

import gov.cdc.hl7.DeIdentifier
import gov.cdc.hl7.RedactInfo
import scala.Tuple2


class Helper {
    fun getRedactedReport(msg: String): Tuple2<String, List<RedactInfo>>? {
        var rules = this::class.java.getResource("/case_config.txt").readText().lines()
        val dIdentifier = DeIdentifier()
        return dIdentifier.deIdentifyMessage(msg, rules.toTypedArray())
    }


}