package gov.cdc.dex.mmg

import com.google.gson.Gson
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.redisModels.ValueSetConcept
import org.junit.jupiter.api.Test


internal class MmgUtilTest {

    @Test
    fun testGetMMGs() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )

        val mmgUtil = MmgUtil(redisProxy)

        println("GenV2")
       val genV2mmgs = mmgUtil.getMMG(MmgUtil.GEN_V2_MMG, null, null, null)
        genV2mmgs.forEach {println(it.name)}

        println("----\nLyme")
        val lymeMMGs = mmgUtil.getMMG(MmgUtil.GEN_V2_MMG, "Lyme_TBRD_MMG_V1.0", "11080", "13")
        lymeMMGs.forEach {println(it.name)}

        println("----\nHepA")
        val hepAMMgs = mmgUtil.getMMG(MmgUtil.GEN_V2_MMG, "Hepatitis_MMG_V1.0", "10110", "21")
        hepAMMgs.forEach {println(it.name)}
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