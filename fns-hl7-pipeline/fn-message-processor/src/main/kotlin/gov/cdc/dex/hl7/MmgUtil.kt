package gov.cdc.dex.hl7

import com.google.gson.Gson

import gov.cdc.dex.hl7.model.MMG
import gov.cdc.dex.hl7.model.ConditionCode
import gov.cdc.dex.hl7.temp.EventCodeUtil
import gov.cdc.hl7.HL7StaticParser
import org.slf4j.LoggerFactory
import java.util.*

import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

class MmgUtil  {
    companion object {
        val logger = LoggerFactory.getLogger(MmgUtil::class.java.simpleName)

        const val PATH_MSH_21_2_1 = "MSH-21[2].1" // Gen 
        const val PATH_MSH_21_3_1 = "MSH-21[3].1" // Condition
        const val EVENT_CODE_PATH = "OBR[@4.1='68991-9']-31.1"

        val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")

        const val REDIS_MMG_PREFIX = "mmg:"
        const val REDIS_CONDITION_PREFIX = "condition:"

        val jedis = Jedis(REDIS_CACHE_NAME, 6380, DefaultJedisClientConfig.builder()
        .password(REDIS_PWD)
        .ssl(true)
        .build())

        private val gson = Gson()

        
        @Throws(Exception::class)
        fun getMMGFromMessage(message: String, filePath: String, messageUUID: String): Array<MMG> {

            val msh21_2 = extractValue(message, PATH_MSH_21_2_1)
            val msh21_3 = extractValue(message, PATH_MSH_21_3_1)
            val eventCode = extractValue(message, EVENT_CODE_PATH)

            logger.info("Info for message filePath ${filePath}, messageUUID: ${messageUUID} --> msh21_2: $msh21_2, msh21_3: $msh21_3, eventCode: $eventCode")

            return getMMG(msh21_2, msh21_3, eventCode)
        } // .getMMGFromMessage

        private fun extractValue(msg: String, path: String):String  {
            val value = HL7StaticParser.getFirstValue(msg, path)
            return if (value.isDefined) value.get() //throw Exception("Error extracting $path from HL7 message")
                else ""
        } // .extractValue


        //TODO:: Add support for others MMG edge cases such as: Foodnet vs FoodBorne MMGs based on reporting jurisdiction, etc..
        @Throws(Exception::class)
        fun getMMG(msh21_2: String, msh21_3: String?, eventCode: String?): Array<MMG> {

            // get the generic MMG:
            val mmg1 = gson.fromJson(jedis.get(msh21_2), MMG::class.java)

            if ( msh21_3.isNullOrEmpty() ) {
                // Only the generic MMG
                 return arrayOf( mmg1 )
            } // .if 
            
            // get the condition code entry 
            val conditionCodeEntry = gson.fromJson(jedis.get(REDIS_CONDITION_PREFIX + eventCode.toString()), ConditionCode::class.java)

            val mmg2KeyName = conditionCodeEntry.mmgMaps.get( msh21_3 )
            val mmg2 = gson.fromJson(jedis.get(mmg2KeyName), MMG::class.java)

            return arrayOf( mmg1, mmg2 )
        } // .getMMG 

    } // .companion

} // .MmgUtil


/* 
MSH-21.1  - PHIN - only for structure validation

MSH-21.2 - MMG Gen  -> GenCaseV1.0, GenV1Summary, GenV2, or ARBO 

MSH-21.3 (only if above is GenV2) - -> Lyme, Perussis, ... 
  - if this is empty only GenV2 from above
  - if not empty, note the mmg for the condition name (such as Lyme_TBRD_MMG_V1.0) then
    - read the condition code from message and read condition code json entry for the condition code 
      - look for the profile name: get the MMG's that go with it. 
        {'event_code': '10250', 
        'name': 'Spotted Fever Rickettsiosis', 
        'program': 'NCEZID', 
        'category': 'Vectorborne Diseases', 
        'mmg_maps': [{'msh_21': 'Lyme_TBRD_MMG_V1.0',  // check this is same as MSH21 (condition spec) - pick the one with same profile 
        
        'mmgs': ['mmg:tbrd']}]}

        mmg_maps: [{"LYME_TBRD_MMG_V1.0": ["mmg:tbrd"] }]

Condition code: e.g. 10250 

TODO: SPECIAL CASES
--------

->   we need 1, 2, or 3 MMG's 

MMG's to use:

1. 
2. 

*/