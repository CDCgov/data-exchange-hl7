package gov.cdc.dex.mrr

import gov.cdc.dex.azure.RedisProxy
import org.junit.jupiter.api.Test

class MMGLoaderTest {

    @Test
    fun testLoadMMGsFromMMGAT() {
        val fn = TimerTriggerFunction()
        val redisName =  System.getenv("REDIS_CACHE_NAME")
        val redisKey = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(redisName, redisKey)
        fn.loadMMGAT(redisProxy)
    }
}