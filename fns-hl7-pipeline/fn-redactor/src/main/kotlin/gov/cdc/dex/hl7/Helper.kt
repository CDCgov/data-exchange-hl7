package gov.cdc.dex.hl7

import gov.cdc.hl7.DeIdentifier
import gov.cdc.hl7.HL7StaticParser
import gov.cdc.hl7.RedactInfo
import scala.Tuple2
import java.util.*


class Helper {
    private val PATH_PID_5_2 = "PID-5[2]"

    fun getRedactedReport(msg: String, messageType: String): Tuple2<String, List<RedactInfo>>? {
        val dIdentifier = DeIdentifier()
        var rules: List<String> = emptyList<String>()

        if(messageType.equals("CASE")) {
            val pid_5_2 = extractValue(msg, PATH_PID_5_2).lowercase(Locale.getDefault())
            rules = if (pid_5_2.equals("^^^^^^S", true)) {
                this::class.java.getResource("/case_config.txt").readText().lines()
            } else {
                this::class.java.getResource("/case_pid_config.txt").readText().lines()
            }
        }else if(messageType.equals("ELR")) {
            val pid_5_2 = extractValue(msg, PATH_PID_5_2).lowercase(Locale.getDefault())
            rules = if (pid_5_2.equals("^^^^^^S", true)) {
                this::class.java.getResource("/lab_config.txt").readText().lines()
            } else {
                this::class.java.getResource("/lab_pid_config.txt").readText().lines()
            }
        }
        return dIdentifier.deIdentifyMessage(msg, rules.toTypedArray())
    }

     fun extractValue(msg: String, path: String):String  {
        val value = HL7StaticParser.getFirstValue(msg, path)
        return if (value.isDefined) value.get() //throw Exception("Error extracting $path from HL7 message")
        else ""
    }

    fun getConfigFileName(msg: String):String{
        val pid_5_2 = extractValue(msg, PATH_PID_5_2).lowercase(Locale.getDefault())
        return if(pid_5_2.equals("^^^^^^S",true)){
            "/case_config.txt"
        } else {
            "/case_pid_config.txt"
        }
    }

   }
