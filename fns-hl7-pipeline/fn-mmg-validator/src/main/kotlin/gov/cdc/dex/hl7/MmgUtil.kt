package gov.cdc.dex.hl7

import com.google.gson.Gson
import gov.cdc.dex.hl7.exception.InvalidMessageException

import gov.cdc.dex.hl7.model.MMG
import gov.cdc.dex.hl7.temp.EventCodeUtil
import gov.cdc.hl7.HL7StaticParser
import org.slf4j.LoggerFactory
import java.util.*

class MmgUtil  {
    companion object {
        val logger = LoggerFactory.getLogger(MmgUtil::class.java.simpleName)
        const val GENV2 = "GENERIC_MMG_V2.0"
        const val GENV1_CASE = "GEN_CASE_MAP_v1.0"
        const val GENV1_SUMMARY = "GEN_SUMMARY_MAP_v1.0"

        const val ARBO = "ARBO_CASE_MAP_V1.0"
        const val GENVx_PROFILE_PATH = "MSH-21[2].1"
        const val CONDITION_PROFILE_PATH = "MSH-21[3].1"
        const val EVENT_CODE_PATH = "OBR[@4.1='68991-9']-31.1"

        private val gson = Gson()

        //TODO:: Download MMGs from REDIS.
        //TODO:: get MMG Name from REDIS table - delete EventCodeUtil.
        //TODO:: Add support for Foodnet vs FoodBorne MMGs based on reporting jurisdiction.
        @Throws(InvalidMessageException::class)
        fun getMMG(genVxMMG: String, conditionMMG: String?, eventCode: String?): Array<MMG> {
            when (genVxMMG.uppercase(Locale.getDefault())) {
               GENV1_CASE.uppercase(Locale.getDefault()), GENV1_SUMMARY.uppercase(Locale.getDefault()) ->  {
                   val genV1 = this::class.java.getResource("/${genVxMMG.uppercase(Locale.getDefault())}.json").readText()
                   return arrayOf(gson.fromJson(genV1, MMG::class.java))
               }
               GENV2.uppercase(Locale.getDefault()) -> {
                   //Need to load GenV2 + potential eventCode specific MMG (when MSH-21[3] is defined)
                   val genV2Config = this::class.java.getResource("/$GENV2.json").readText()
                   val genV2 = gson.fromJson(genV2Config, MMG::class.java)
                   if (conditionMMG != null) {
                       val conditionSpecificConfig =
                           this::class.java.getResource("/${eventCode?.let { EventCodeUtil.getMMGName(it) }}.json")?.readText()
                       val condition = gson.fromJson(conditionSpecificConfig, MMG::class.java)
                       if (condition ===null) {
                           throw InvalidMessageException("Unable to find MMG for event code $eventCode")
                       }
                       return arrayOf(genV2, condition)
                   }
                   return arrayOf(genV2)
               }
               ARBO.uppercase(Locale.getDefault()) -> {
                   val arboConfig = this::class.java.getResource("/ARBO_1.3.json").readText()
                   return arrayOf(gson.fromJson(arboConfig, MMG::class.java))
               }
                else -> throw InvalidMessageException("No MMG available for profile $genVxMMG and event code $eventCode")
           }


        }
        @Throws(InvalidMessageException::class)
        fun getMMGFromMessage(message: String): Array<MMG> {
            val genVProfile = extractValue(message, GENVx_PROFILE_PATH)
            val conditionProfile = extractValue(message, CONDITION_PROFILE_PATH)
            val eventCode = extractValue(message, EVENT_CODE_PATH)
            logger.info("Profiles:\nGenV2: $genVProfile\nCondition Specific: $conditionProfile\nEvent Code:$eventCode")

            return getMMG(genVProfile, conditionProfile, eventCode)

        }

        private fun extractValue(msg: String, path: String):String  {
            val value = HL7StaticParser.getFirstValue(msg, path)
            return if (value.isDefined) value.get() //throw InvalidMessageException("Error extracting $path from HL7 message")
                else ""
        }
    }
} // .MmgUtil