package gov.cdc.dex.mrr

import gov.cdc.dex.azure.RedisProxy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EventCodeClientTest {
    private lateinit var redisProxy:RedisProxy

    @BeforeAll
    internal fun initRedisProxy() {
        val redisName =  System.getenv("REDIS_CACHE_NAME")
        val redisKey = System.getenv("REDIS_CACHE_KEY")
        redisProxy = RedisProxy(redisName, redisKey)
    }
    @Test
    fun loadEventMapsTest() {
        val eventCodes  = EventCodeClient()
        eventCodes.loadEventMaps(redisProxy)
    }
    @Test
    fun loadGroupsTest() {
        val groupClient  = EventCodeClient()
        groupClient.loadGroups(redisProxy)
    }
}