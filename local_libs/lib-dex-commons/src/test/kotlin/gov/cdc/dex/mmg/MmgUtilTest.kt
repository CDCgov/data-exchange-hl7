package gov.cdc.dex.mmg

import gov.cdc.dex.azure.RedisProxy
import org.junit.jupiter.api.Test


internal class MmgUtilTest {

    @Test
    fun testQueryTableFor() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )

        val mmgs = MmgUtil(redisProxy).queryTableFor("10065", "arbo_case_map_v1.0") //"lyme_tbrd_mmg_v1.0")
       // println(mmgs)
        mmgs.forEach { println(it.name)}
    }
}