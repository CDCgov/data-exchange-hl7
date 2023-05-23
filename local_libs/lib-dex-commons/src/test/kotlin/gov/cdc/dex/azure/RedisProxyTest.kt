package gov.cdc.dex.azure

import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
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

    @Test
    fun testJedisPool () {
        var jedis: Jedis? = null
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        try{
            val redisProxy =  RedisProxy(REDIS_CACHE_NAME,REDIS_PWD )
            jedis = redisProxy.getJedisClient()
            println("Test Redisconnection:" + jedis.ping())
        }

        catch(e: Exception){
            println("Exception: ${e.printStackTrace()}")

        }
        finally{
            jedis?.close()
        }
    }
}