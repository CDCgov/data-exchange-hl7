package gov.cdc.dex.azure

import org.slf4j.LoggerFactory
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

class RedisProxy {
    companion object {
        val REDIS_CACHE_NAME: String = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD: String =        System.getenv("REDIS_CACHE_KEY")
        val REDIS_PORT: String   =     System.getenv("REDIS_PORT") ?: "6380"
    }
    private val jedis = Jedis(REDIS_CACHE_NAME, REDIS_PORT.toInt(), DefaultJedisClientConfig.builder()
        .password(REDIS_PWD)
        .ssl(true)
        .build()
    )
    private val logger = LoggerFactory.getLogger(RedisProxy::class.java.simpleName)

    fun getJedisClient(): Jedis {
        return jedis
    }
}