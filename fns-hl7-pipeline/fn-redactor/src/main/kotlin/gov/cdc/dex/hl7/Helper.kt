package gov.cdc.dex.hl7

import gov.cdc.hl7.DeIdentifier
import gov.cdc.hl7.RedactInfo
import scala.Tuple2


class Helper {
    fun getRedactedReport(msg :String): Tuple2<String,List<RedactInfo>>? {
      println("a")
        var rules = this::class.java.getResource("/case_config.txt").readText().lines()
        println("rules:${rules}")
        val dIdentifier = DeIdentifier()
       val report = dIdentifier.deIdentifyMessage(msg, rules.toTypedArray())
       return report
  }


}