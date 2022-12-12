package gov.cdc.dex.mmg

import com.google.gson.Gson
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.redisModels.ValueSetConcept
import org.junit.jupiter.api.Test


internal class MmgUtilTest {

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

        println("GenV2")
       val genV2mmgs = mmgUtil.getMMGs(MmgUtil.GEN_V2_MMG, null, null, null)
        genV2mmgs.forEach {println(it.name)}
        assert(genV2mmgs.size == 1)

        println("----\nLyme")
        val lymeMMGs = mmgUtil.getMMGs(MmgUtil.GEN_V2_MMG, "Lyme_TBRD_MMG_V1.0", "11080", "13")
        lymeMMGs.forEach {println(it.name)}
        assert(lymeMMGs.size == 2)

        println("----\nHepA")
        val hepAMMgs = mmgUtil.getMMGList(MmgUtil.GEN_V2_MMG, "Hepatitis_MMG_V1.0", "10110", "21")
        hepAMMgs.forEach {println(it)}
        assert(hepAMMgs.size == 3)

        println("----\nArbo regular")
        val arboMMgs = mmgUtil.getMMGList(MmgUtil.ARBO_MMG_v1_0, "", "10058", "21")
        arboMMgs.forEach { println(it) }
        assert(arboMMgs.size == 1)
        assert(arboMMgs[0] == "mmg:arboviral_v1_3_2_mmg_20210721")

        println("----\nArbo special")
        val arboMMgS = mmgUtil.getMMGList(MmgUtil.ARBO_MMG_v1_0, "", "10058", "23")
        arboMMgS.forEach { println(it) }
        assert(arboMMgS.size == 1)
        assert(arboMMgS[0] == "mmg:arboviral_human_case_message_mapping_guide")

        try {
            mmgUtil.getMMGList(MmgUtil.GEN_V2_MMG, "Lyme_TBRD_MMG_V1.0", "1108", "21")
            assert(false)
        } catch (e: InvalidConditionException) {
            assert(true)
            println("Exception properly thrown: ${e.message}")
        }
    }

    @Test
    fun testRedisKeys() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )

        val keys = redisProxy.getJedisClient().keys("mmg:*")
        println(keys)
        val mapping = redisProxy.getJedisClient().get("condition:10110")
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
}