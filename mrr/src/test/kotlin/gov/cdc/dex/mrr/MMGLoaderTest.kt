package gov.cdc.dex.mrr

import gov.cdc.dex.azure.RedisProxy
import org.junit.jupiter.api.Test

class MMGLoaderTest {
    val redisName =  System.getenv("REDIS_CACHE_NAME")
    val redisKey = System.getenv("REDIS_CACHE_KEY")
    @Test
    fun testLoadMMGsFromMMGAT() {
        val fn = MmgatClient()
        val redisProxy = RedisProxy(redisName, redisKey)
       // fn.loadLegacyMmgat(redisProxy)
        fn.loadMMGAT(redisProxy)
    }

    @Test
    fun loadLegacies() {
        val fn = MmgatClient()
        val redisProxy = RedisProxy(redisName, redisKey)
        fn.loadLegacyMmgat(redisProxy)
    }


}