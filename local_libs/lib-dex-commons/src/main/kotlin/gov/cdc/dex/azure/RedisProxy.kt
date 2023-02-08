package gov.cdc.dex.azure

import org.slf4j.LoggerFactory
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis
import java.io.Closeable

class RedisProxy( redisName: String,  redisKey:String,  redisPort: Int = 6380) : Closeable {
    companion object {
        const val REDIS_CACHE_NAME_PROP_NAME: String = "REDIS_CACHE_NAME"
        const val REDIS_PWD_PROP_NAME: String        = "REDIS_CACHE_KEY"
        const val REDIS_PORT_PROP_NAME: String       = "REDIS_PORT"
    }
    private val logger = LoggerFactory.getLogger(RedisProxy::class.java.simpleName)

    private val jedis = Jedis(redisName, redisPort, DefaultJedisClientConfig.builder()
        .password(redisKey)
        .ssl(true)
        .timeoutMillis(400000)
        .build()
    )
    init {
        logger.info("REDIS connection established with $redisName")
    }

    fun getJedisClient(): Jedis {
        return jedis
    }

    override fun close() {
        jedis.close()
    }
}