package gov.cdc.dex.mmg

import com.google.gson.Gson
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.redisModels.Condition2MMGMapping
import gov.cdc.dex.redisModels.ValueSetConcept
import gov.cdc.dex.util.JsonHelper.gson
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals


internal class MmgUtilTest {
    private val ARBO_MMG_v1_0 = "arbo_case_map_v1_0"
    private val VAR_MMG_v2_0 = "var_case_map_v2_0"
    @Test
    fun testGetMessageInfo() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )
        val mmgUtil = MmgUtil(redisProxy)
        try {
            // 11085 should come under Genv1 or Genv2, not Arboviral
            val messageInfo = mmgUtil.getMMGMessageInfo(ARBO_MMG_v1_0, null, "11085", "23")
        } catch (e : InvalidConditionException) {
            println("Exception correctly thrown: ${e.message}")
        }

        val lymeMessageInfo = mmgUtil.getMMGMessageInfo(MmgUtil.GEN_V2_MMG, "Lyme_TBRD_MMG_V1.0", "11080", "13")
        println(lymeMessageInfo)

        val arboSpecialInfo = mmgUtil.getMMGMessageInfo(ARBO_MMG_v1_0, null, "10058", "23")
        println(arboSpecialInfo)


    }
    @Test
    fun testLoadMMGfromMessage() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )
        val mmgUtil = MmgUtil(redisProxy)

        val mmgsArr = mmgUtil.getMMGs("Var_Case_Map_v2.0","","10030", "13")

        mmgsArr.forEach {
            println("MMG ID: ${it.id}, NAME: ${it.name}, BLOCKS: --> ${it.blocks.size}")
        }

        assertEquals(mmgsArr.size, 1)
        // assertEquals(mmgs[0].blocks.size + mmgs[1].blocks.size, 7 + 26) // the Message Header is trimmed from the GenV2 hence 7 and not 8
    } // .testLoadMMGfromMessage
    @Test
    fun testGroupValues() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )
        val codeInSet = "06"
        val codeNotInSet = "10"
        val groupFoodNet = redisProxy.getJedisClient().smembers("group:foodnet_states")
        println(groupFoodNet)
        var codeExists = redisProxy.getJedisClient().sismember("group:foodnet_states", codeInSet)
        assert(codeExists)
        codeExists = redisProxy.getJedisClient().sismember("group:foodnet_states", codeNotInSet)
        assert(!codeExists)
    }
    @Test
    fun testGetMMGs() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )

        val mmgUtil = MmgUtil(redisProxy)

//        println("GenV2") -- EVENT CODE CANNOT BE NULL
//       val genV2mmgs = mmgUtil.getMMGs(MmgUtil.GEN_V2_MMG, null, null, null)
//        genV2mmgs.forEach {println(it.name)}
//        assert(genV2mmgs.size == 1)

        println("----\nLyme")
        val lymeMMGs = mmgUtil.getMMGs(MmgUtil.GEN_V2_MMG, "Lyme_TBRD_MMG_V1.0", "11080", "13")
        lymeMMGs.forEach {println(it.name)}
        assert(lymeMMGs.size == 2)

        println("----\nHepA")
        val hepAMMgs = mmgUtil.getMMGList(MmgUtil.GEN_V2_MMG, "Hepatitis_MMG_V1.0", "10110", "21")
        hepAMMgs.forEach {println(it)}
        assert(hepAMMgs.size == 3)

        println("----\nArbo regular")
        val arboMMgs = mmgUtil.getMMGList(ARBO_MMG_v1_0, "", "10058", "21")
        arboMMgs.forEach { println(it) }
        assert(arboMMgs.size == 1)
        assert(arboMMgs[0] == "mmg:arboviral_v1_3_2_mmg_20210721")

        println("----\nArbo special")
        val arboMMgS = mmgUtil.getMMGList(ARBO_MMG_v1_0, "", "10058", "23")
        arboMMgS.forEach { println(it) }
        assert(arboMMgS.size == 1)
        assert(arboMMgS[0] == "mmg:arboviral_human_case_message_mapping_guide")

        println("----\nVaricella v2")
        val varMMgS = mmgUtil.getMMGList(VAR_MMG_v2_0, "", "10030", "23")
        varMMgS.forEach { println(it) }
        assert(varMMgS.size == 1)
        assert(varMMgS[0] == "mmg:varicella_message_mapping_guide_v2_01")

        try {
            mmgUtil.getMMGList(MmgUtil.GEN_V2_MMG, "Lyme_TBRD_MMG_V1.0", "1108", "21")
            assert(false)
        } catch (e: InvalidConditionException) {
            assert(true)
            println("Exception properly thrown: ${e.message}")
        }
    }

    @Test
    fun testRemoveDuplicteMSH21() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )

        val mmgUtil = MmgUtil(redisProxy)
        val lymeMMGs = mmgUtil.getMMGs(MmgUtil.GEN_V2_MMG, "Lyme_TBRD_MMG_V1.0", "11080", "13")

        assert(lymeMMGs.size == 2)

        val msh21GenV2 = lymeMMGs[0].blocks.filter { it.name == "Message Header" } //Should not be present.
        assert(msh21GenV2.isEmpty())

        val msh21Lyme = lymeMMGs[1].blocks.filter { it.name == "MSH Header" }
        assert(msh21Lyme.size == 1)

    }

    @Test
    fun testRedisKeys() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )

        val keys = redisProxy.getJedisClient().keys("mmg:*")
        println(keys)
        val mapping = redisProxy.getJedisClient().get("condition:10530")
        println(mapping)
    }
    @Test
    fun testGetPhinVads() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )

        val vsJson = redisProxy.getJedisClient().hget("vocab:PHVS_YesNoUnknown_CDC", "Y")
        println(vsJson)

        val vsObj = Gson().fromJson(vsJson, ValueSetConcept::class.java)
        println(vsObj)

    }

    @Test
    fun listAllPossibleRoutes() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )
        val eventCodes = redisProxy.getJedisClient().keys("${MmgUtil.REDIS_CONDITION_PREFIX}*"  )
        val routeList = mutableListOf<String?>()
        eventCodes.forEach {
            val code = gson.fromJson( redisProxy.getJedisClient().get( it) , Condition2MMGMapping::class.java)
            code.profiles?.forEach { profile ->
                val route = profile.mmgs?.last()?.replace(MmgUtil.REDIS_MMG_PREFIX, "")
                if (!routeList.contains(route)) {
                    routeList.add(route)
                }
                profile.specialCases?.forEach { specialCase ->
                    val route2 = specialCase.appliesTo.replace(MmgUtil.REDIS_GROUP_PREFIX, "") + "_" + specialCase.mmgs?.last()?.replace(MmgUtil.REDIS_MMG_PREFIX, "")
                    if (!routeList.contains(route2)) {
                        routeList.add(route2)
                    }
                }
            }
        }
        println(routeList)
    }

    @Test
    fun testGetMMGInfoForBotulism() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )

        val mmgUtil = MmgUtil(redisProxy)
        val msgInfo = mmgUtil.getMMGMessageInfo("Gen_Case_Map_v1.0", null, "10530", "13")
        print(msgInfo)
    }
}