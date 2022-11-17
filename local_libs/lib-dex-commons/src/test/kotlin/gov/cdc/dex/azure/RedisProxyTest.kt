package gov.cdc.dex.azure

import org.junit.jupiter.api.Test
import redis.clients.jedis.exceptions.JedisConnectionException

class RedisProxyTest {

    @Test
    fun testLoadRedisWithoutProps() {
        try {
            val redisProxy =  RedisProxy("", "" )
        } catch (e: JedisConnectionException) {
            assert(true)
            println(e.message)
        }
    }

    @Test
    fun testLoadRedisWithProps() {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )
        assert(true)
    }
}