package gov.cdc.dex.mmg

import com.google.gson.Gson
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.metadata.DexMessageInfo
import gov.cdc.dex.metadata.HL7MessageType
import gov.cdc.dex.redisModels.Condition2MMGMapping
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.redisModels.SpecialCase
import gov.cdc.dex.util.StringUtils.Companion.normalize
import org.slf4j.LoggerFactory


class MmgUtil(val redisProxy: RedisProxy)  {
    companion object {
        val logger = LoggerFactory.getLogger(MmgUtil::class.java.simpleName)
        const val GEN_V2_MMG = "generic_mmg_v2_0"
        const val REDIS_MMG_PREFIX = "mmg:"
        const val REDIS_CONDITION_PREFIX = "condition:"
        const val REDIS_GROUP_PREFIX = "group:"

        private val gson = Gson()
        private val legacy_mmgs = listOf("arbo_case_map_v1_0",
                                        "var_case_map_v1_0",
                                        "var_case_map_v2_0",
                                        "tb_case_map_v2_0")
    }

    @Throws(InvalidConditionException::class)
    fun getMMGList(msh21_2: String, msh21_3: String?, eventCode: String, jurisdictionCode: String?): Array<String> {
        val messageInfo = getMMGMessageInfo(msh21_2, msh21_3, eventCode, jurisdictionCode)
        return messageInfo.mmgKeyList?.toTypedArray() ?: arrayOf()
    }


    @Throws(InvalidConditionException::class)
    fun getMMGs(mmgKeyList: Array<String>): Array<MMG> {
        var mmgs : Array<MMG> = arrayOf()
        for (keyName: String in mmgKeyList) {
            val mmg1 = gson.fromJson(redisProxy.getJedisClient().get(keyName), MMG::class.java)
            //REMOVE MSH-21 from GenV2: when condition specific is also defining it with new cardinality of [3..3]
            if (keyName.contains(GEN_V2_MMG)) {
                mmg1.blocks = mmg1.blocks.filter { it.name != "Message Header" }
            }
            mmgs += mmg1
        }
        return mmgs
     }

    @Throws(InvalidConditionException::class)
    fun getMMGs(msh21_2: String, msh21_3: String?, eventCode: String, jurisdictionCode: String?): Array<MMG> {
        val mmgList = getMMGList(msh21_2, msh21_3, eventCode, jurisdictionCode)
        return getMMGs(mmgList)
    }

    @Throws(InvalidConditionException::class)
    // Populates DexMessageInfo, including list of MMGs and provision route.
    fun getMMGMessageInfo(msh21_2: String, msh21_3: String?, eventCode: String, jurisdictionCode: String?): DexMessageInfo {
        val messageInfo = DexMessageInfo(eventCode, null, null, jurisdictionCode, HL7MessageType.CASE)
        // list of mmg keys to look up in redis
        var mmg2KeyNames = arrayOf<String>()
        // condition-specific profile from message
        var msh21_3In = msh21_3?.normalize()

        if (msh21_2.normalize() in legacy_mmgs) {
            // msh_21_3 is empty, make it same as msh_21_2
            msh21_3In = msh21_2.normalize()
        } else {
            // add the Generic MMG
            mmg2KeyNames += REDIS_MMG_PREFIX +  msh21_2.normalize()
        } // .else

        // get the condition code entry. Needed for both routing and mmgs
        val eventCodeEntry =
            gson.fromJson( redisProxy.getJedisClient().get(REDIS_CONDITION_PREFIX + eventCode) , Condition2MMGMapping::class.java)
                ?: throw InvalidConditionException("Condition $eventCode not found in mapping table.")

        if (!msh21_3In.isNullOrEmpty()  && !eventCodeEntry.profiles.isNullOrEmpty()) {
            // filter condition's profiles for a match on msh-21[3]
            var specialCaseAdded = false
            val profileMatches = eventCodeEntry.profiles.filter { profile -> profile.name == msh21_3In }
            if (profileMatches.isNotEmpty()) {
                val profile = profileMatches[0]
                // look at special cases first, if any exist
                if (!profile.specialCases.isNullOrEmpty()) {
                    for (case: SpecialCase in profile.specialCases) {
                        // see if the jurisdiction code is a member of the group to which this case applies
                        val appliesHere = redisProxy.getJedisClient().sismember(case.appliesTo, jurisdictionCode)
                        if (appliesHere) {
                            mmg2KeyNames += case.mmgs //returns a list of mmg keys
                            messageInfo.route = "${mmg2KeyNames.last().replace(REDIS_MMG_PREFIX, "")}_${case.appliesTo.replace(REDIS_GROUP_PREFIX, "")}"
                            specialCaseAdded = true
                            break
                        }
                    } // .for
                } // .if special cases

                if (!specialCaseAdded) {
                    // no special cases apply; use the regular mmgs for this profile+condition
                    mmg2KeyNames += profile.mmgs
                    messageInfo.route = mmg2KeyNames.last().replace(REDIS_MMG_PREFIX, "")
                }
            } else {
                throw InvalidConditionException("Condition $eventCode does not match the profile $msh21_3In.")
            }
        } else {
            // no condition-specific MMGs -- route to Generic
            messageInfo.route = "${msh21_2.normalize()}_${eventCodeEntry.category.normalize()}"
        }
        messageInfo.mmgKeyList = mmg2KeyNames.toList()
        return messageInfo
    }
} // // .MmgUtil

//}



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


--------

->   we need 1, 2, or 3 MMG's 

MMG's to use:

1. 
2. 

*/