package gov.cdc.dex.azure

import org.slf4j.LoggerFactory
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

class RedisProxy(val redisName: String, val redisKey:String, val redisPort: Int = 6380) {
    companion object {
        val REDIS_CACHE_NAME_PROP_NAME: String = "REDIS_CACHE_NAME"
        val REDIS_PWD_PROP_NAME: String =        "REDIS_CACHE_KEY"
        val REDIS_PORT_PROP_NAME: String   =     "REDIS_PORT"
    }
    private val jedis = Jedis(redisName, redisPort.toInt(), DefaultJedisClientConfig.builder()
        .password(redisKey)
        .ssl(true)
        .build()
    )
    private val logger = LoggerFactory.getLogger(RedisProxy::class.java.simpleName)

    fun getJedisClient(): Jedis {
        return jedis
    }
}