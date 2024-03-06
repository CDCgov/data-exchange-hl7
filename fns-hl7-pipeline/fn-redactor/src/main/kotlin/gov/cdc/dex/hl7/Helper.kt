package gov.cdc.dex.hl7

import gov.cdc.hl7.DeIdentifier
import gov.cdc.hl7.HL7StaticParser
import gov.cdc.hl7.RedactInfo
import scala.Tuple2
import java.io.FileNotFoundException


class Helper {

    fun getRedactedReport(msg: String, route: String = "elr"): Tuple2<String, List<RedactInfo>>? {
        val dIdentifier = DeIdentifier()
        val configFile = "/${getConfigFileName(route)}"
        val rules = if (this::class.java.getResource(configFile) != null) {
            this::class.java.getResource(configFile)!!.readText().lines()
        } else {
            throw FileNotFoundException("Unable to find redaction config file $configFile")
        }
        return dIdentifier.deIdentifyMessage(msg, rules.toTypedArray())
    }

     fun extractValue(msg: String, path: String) : String  {
        val value = HL7StaticParser.getFirstValue(msg, path)
        return if (value.isDefined) value.get() //throw Exception("Error extracting $path from HL7 message")
        else ""
    }

    fun getConfigFileName(route:String) : String{
        val fileSuffix = "_config.txt"
        val routeConfigFile = "${route.lowercase()}$fileSuffix"
        val typeConfigFile = "elr$fileSuffix"
        // return without the '/' in front of the name for use in metadata
        return if (route.isNotEmpty() && this::class.java.getResource("/$routeConfigFile") != null) {
            routeConfigFile
        } else {
            typeConfigFile
        }
    }

   }
