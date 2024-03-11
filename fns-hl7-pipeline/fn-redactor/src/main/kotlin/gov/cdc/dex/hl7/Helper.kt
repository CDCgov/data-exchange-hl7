package gov.cdc.dex.hl7

import gov.cdc.dex.util.InvalidMessageException
import gov.cdc.dex.util.ProfileConfiguration
import gov.cdc.hl7.DeIdentifier
import gov.cdc.hl7.HL7StaticParser
import gov.cdc.hl7.RedactInfo
import scala.Tuple2
import java.io.FileNotFoundException
import java.util.NoSuchElementException


class Helper {

    fun getRedactedReport(msg: String, configFileName: String): Tuple2<String, List<RedactInfo>>? {
        val dIdentifier = DeIdentifier()
        val configFile = "/profiles/$configFileName"
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

    fun getConfigFileName(hl7Content: String, profileConfig: ProfileConfiguration, dataStreamId: String ) : String{
        val fileSuffix = "-config.txt"
        val dataStreamName = dataStreamId.uppercase().trim()
        val profileList = profileConfig.profileIdentifiers.filter {
            it.dataStreamId.uppercase().trim() == dataStreamName
        }
        val dataStreamConfigFile = if (profileList.isNotEmpty()) {
            val profileIdPaths = profileList[0].identifierPaths
            val profileName = try {
                "$dataStreamName-" + profileIdPaths.map { path ->
                    HL7StaticParser.getFirstValue(hl7Content, path).get().uppercase()
                }.reduce { acc, map -> "$acc-$map" }
            } catch (e: NoSuchElementException) {
                throw InvalidMessageException(
                    "Unable to load redaction profile for data stream id $dataStreamId: " +
                            "One or more values in the profile path(s)" +
                            " ${profileIdPaths.joinToString()} are missing."
                )
            }
            "$profileName$fileSuffix"
        } else {
            "$dataStreamName$fileSuffix"
        }
        // return without the '/' in front of the name for use in metadata
        return if (this::class.java.getResource("/profiles/$dataStreamConfigFile") != null) {
            dataStreamConfigFile
        } else {
            // Use default (ELR) config if nothing else exists
            "DEFAULT$fileSuffix"
        }
    }

   }
