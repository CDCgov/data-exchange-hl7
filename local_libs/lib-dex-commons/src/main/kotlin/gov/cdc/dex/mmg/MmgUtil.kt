package gov.cdc.dex.mmg

import com.google.gson.Gson
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.redisModels.Condition2MMGMapping

import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.redisModels.SpecialCase
import gov.cdc.dex.util.StringUtils.Companion.normalize

import org.slf4j.LoggerFactory



class MmgUtil(val redisProxy: RedisProxy)  {
    companion object {
        val logger = LoggerFactory.getLogger(MmgUtil::class.java.simpleName)

        const val MSH_21_2_1_PATH = "MSH-21[2].1" // Gen
        const val MSH_21_3_1_PATH = "MSH-21[3].1" // Condition
        const val EVENT_CODE_PATH = "OBR[@4.1='68991-9']-31.1"
        const val JURISDICTION_CODE_PATH = "OBX[@3.1='77966-0']-5.1" // TODO: complete path

        const val GEN_V2_MMG = "generic_mmg_v2.0"
        const val ARBO_MMG_v1_0 = "arbo_case_map_v1.0"

        const val REDIS_MMG_PREFIX = "mmg:"
        const val REDIS_CONDITION_PREFIX = "condition:"

        private val gson = Gson()
    }



    @Throws(Exception::class)
    fun getMMG(msh21_2: String, msh21_3: String?, eventCode: String?, jurisdictionCode: String?): Array<MMG> {
        // TODO : include jurisdictionCode logic

        //val mmg1 : MMG
        // make a list to hold all the mmgs we need
        var mmgs : Array<MMG> = arrayOf()

        var msh21_3In = msh21_3?.normalize()

        if (msh21_2.normalize().contains(ARBO_MMG_v1_0)) {
            // msh_21_3 is empty, make it same as msh_21_2
            msh21_3In = msh21_2.normalize()

        } else {
            // get the generic MMG:
            // TODO: add exceptions // logger.info("Pulling MMG: key: ${rKey}")
            val mmg1 = gson.fromJson(redisProxy.getJedisClient().get(REDIS_MMG_PREFIX + msh21_2.normalize()), MMG::class.java)

            //REMOVE MSH-21 from GenV2: when condition specific is also defining it with new cardinality of [3..3]
            if (GEN_V2_MMG == msh21_2.normalize() && eventCode != null) {
                mmg1.blocks = mmg1.blocks.filter { it.name != "Message Header" }
            }
            mmgs += mmg1

        } // .else

        if ( !msh21_3In.isNullOrEmpty() ) {
            // get the condition code entry
            val eventCodeEntry =
                gson.fromJson( redisProxy.getJedisClient().get(REDIS_CONDITION_PREFIX + eventCode) , Condition2MMGMapping::class.java)
            // check if there are mmg:<name> maps for this condition
            if ( eventCodeEntry != null && !eventCodeEntry.mmgMaps.isNullOrEmpty() ) {
                var mmg2KeyNames: List<String>? = null
                // look at special cases first, if any exist
                if (! eventCodeEntry.specialCases.isNullOrEmpty()) {
                    // specialCases is a list
                    for (case: SpecialCase in eventCodeEntry.specialCases) {
                        // see if the jurisdiction code is a member of the group to which this case applies
                        val appliesHere = redisProxy.getJedisClient().sismember(case.appliesTo, jurisdictionCode)
                        if (appliesHere && !case.mmgMaps[msh21_3In].isNullOrEmpty()) {
                            mmg2KeyNames = case.mmgMaps[msh21_3In] //returns a list of mmg keys
                        }

                    } // .for
                } // .if

                if (mmg2KeyNames.isNullOrEmpty()) {
                    // no special case matched this jurisdiction, so get the regular mmg map list for the profile
                    mmg2KeyNames = eventCodeEntry.mmgMaps[msh21_3In]  //returns a list of mmg keys
                }

                // add the condition-specific mmgs to the list
                if (! mmg2KeyNames.isNullOrEmpty()) {
                    for (keyName: String in mmg2KeyNames) {
                        mmgs += gson.fromJson(redisProxy.getJedisClient().get(keyName), MMG::class.java)
                    }
                }

            } // .if

        } // .if

        return mmgs
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