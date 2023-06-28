package gov.cdc.dex.azure

import gov.cdc.dex.util.StringUtils.Companion.getOrDefault
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import redis.clients.jedis.exceptions.JedisConnectionException
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

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

    @OptIn(ExperimentalTime::class)
    @Test
    fun testJedisPool () {
//        var jedis: Jedis? = null
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME, REDIS_PWD)
        val totaltime = measureTime {
            println("Redis Proxy to use $REDIS_CACHE_NAME")
            for (i in 1..500) {
                val time = measureTime {
                    redisProxy.getJedisClient().use { jedis -> jedis.ping() }
                }
                if (time.inWholeMilliseconds > 1000) {
                    println("iteration $i took $time.")
                }

            }
            println("borrowed count:  ${redisProxy.jedisPool.borrowedCount}")
            println("destroyed count: ${redisProxy.jedisPool.destroyedCount}")
        }
        println("test took $totaltime.")

    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testJedisPoolReuseConn () {
//        var jedis: Jedis? = null
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")

        val redisProxy = RedisProxy(REDIS_CACHE_NAME, REDIS_PWD)
        println("Redis Proxy to use $REDIS_CACHE_NAME")
        val time = measureTime {
            redisProxy.getJedisClient().use { jedis ->
                for (i in 1..500) {
                     jedis.ping() }
                }
        }
        println("all pings took $time.")
        println("borrowed count:  ${redisProxy.jedisPool.borrowedCount}")
        println("destroyed count: ${redisProxy.jedisPool.destroyedCount}")
    }

    @Test
    fun testGetEnvValueWithDefault() {
        val unk = System.getenv("no_variable").getOrDefault("20")
        println(unk)
        val valid = System.getenv("VALID_ENV_NAME").getOrDefault("Not this value")
        println(valid)
    }
}