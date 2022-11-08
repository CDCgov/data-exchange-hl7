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

            // make sure the profile names we have received are lowercase
            val l_msh_21_2 = msh21_2.lowercase(Locale.getDefault())
            var l_msh_21_3 = msh21_3?.lowercase(Locale.getDefault())
            val mmg1 : MMG
            // make a list to hold all the mmgs we need
            val mmgs : MutableList<MMG>

            if (l_msh_21_2.contains("arbo_case_map_v1.0")) {
                l_msh_21_3 = l_msh_21_2
            } else {
                // get the generic MMG:
                mmg1 = gson.fromJson(jedis.get(l_msh_21_2), MMG::class.java)
                mmgs.add(mmg1)
            }

            if ( msh21_3.isNullOrEmpty() ) {
                // Only the generic MMG
                 return arrayOf( mmg1 )
            } // .if 
            
            // get the condition code entry 
            val eventCodeEntry = gson.fromJson(jedis.get(REDIS_CONDITION_PREFIX + eventCode.toString()), ConditionCode::class.java)
            // get the mmg:<name> redis keys for the msh-21 profile for this condition
            var mmg2KeyNames = eventCodeEntry.mmgMaps[l_msh_21_3]  //returns a list of mmg keys
            // add the condition-specific mmgs to the list
            if (mmg2KeyNames != null) {
                for (keyName : String in mmg2KeyNames) {
                    mmgs.add(gson.fromJson(jedis.get(keyName), MMG::class.java))
                }
            }
            return mmgs.toTypedArray()
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