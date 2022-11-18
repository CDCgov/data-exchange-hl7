package gov.cdc.dex.mmg

import com.google.gson.Gson
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.redisModels.ConditionCode

import gov.cdc.dex.redisModels.MMG

import org.slf4j.LoggerFactory
import java.util.*

import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

class MmgUtil(val redisProxy: RedisProxy)  {
    companion object {
        val logger = LoggerFactory.getLogger(MmgUtil::class.java.simpleName)

        const val PATH_MSH_21_2_1 = "MSH-21[2].1" // Gen 
        const val PATH_MSH_21_3_1 = "MSH-21[3].1" // Condition
        const val EVENT_CODE_PATH = "OBR[@4.1='68991-9']-31.1"
        const val PATH_JURISDICTION_CODE = "OBX[@3.1='77966-0']-5.1" // TODO: complete path

        const val GEN_V2_MMG = "Generic_MMG_V2.0"

        const val REDIS_MMG_PREFIX = "mmg:"
        const val REDIS_CONDITION_PREFIX = "condition:"

        private val gson = Gson()
    }

    //TODO:: Add support for others MMG edge cases such as: Foodnet vs FoodBorne MMGs based on reporting jurisdiction, etc..
    @Throws(Exception::class)
    fun getMMG(msh21_2: String, msh21_3: String?, eventCode: String, jurisdictionCode: String?): Array<MMG> {
        // TODO : include jurisdictionCode logic
        // make a list to hold all the mmgs we need
        val mmgs : MutableList<MMG> = mutableListOf()
        if (msh21_2 == GEN_V2_MMG) {
            val mmg1 = gson.fromJson(redisProxy.getJedisClient().get(REDIS_MMG_PREFIX + GEN_V2_MMG), MMG::class.java)
            mmgs.add(mmg1)
        } else {
            mmgs.addAll(queryTableFor(eventCode, msh21_2))
        }
        if (msh21_3 != null) {
            mmgs.addAll(queryTableFor(eventCode, msh21_3))
        }
        return mmgs.toTypedArray()
    } // .getMMG

     fun queryTableFor(eventCode: String, guide: String): List<MMG> {
         val mmgs: MutableList<MMG> = mutableListOf()
         val eventCodeEntry =
             gson.fromJson(
                 redisProxy.getJedisClient().get(MmgUtil.REDIS_CONDITION_PREFIX + eventCode),
                 ConditionCode::class.java
             )
         // get the mmg:<name> redis keys for the msh-21 profile for this condition

         if (!eventCodeEntry.mmgMaps.isNullOrEmpty()) {
             val mmg2KeyNames = eventCodeEntry.mmgMaps[guide]  //returns a list of mmg keys
             // add the condition-specific mmgs to the list
             if (mmg2KeyNames != null) {
                 for (keyName: String in mmg2KeyNames) {
                     mmgs.add(gson.fromJson(redisProxy.getJedisClient().get(keyName), MMG::class.java))
                 }
             }

         } // .
         return mmgs.toList()
     }
} // .companion

//} // .MmgUtil



// val mmgsKeyList = eventCodeEntry.mmgMaps.get( msh21_3 )

// var mmgList = arrayOf(mmg1)

// if ( !mmgsKeyList.isNullOrEmpty() ) {
//     mmgsKeyList.forEach { key -> 
//         mmgList += gson.fromJson(jedis.get(key), MMG::class.java)
//     } // forEach
// } // .if 


// return mmgList


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