package gov.cdc.dex.azure

import org.slf4j.LoggerFactory
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.io.Closeable

class RedisProxy( redisName: String,  redisKey:String,  redisPort: Int = 6380)  {
    companion object {
        const val REDIS_CACHE_NAME_PROP_NAME: String = "REDIS_CACHE_NAME"
        const val REDIS_PWD_PROP_NAME: String        = "REDIS_CACHE_KEY"
        const val REDIS_PORT_PROP_NAME: String       = "REDIS_PORT"
        val jedisPoolConfig = JedisPoolConfig().apply {
            maxTotal = 300
            maxIdle = 20
            minIdle = 10
            testOnBorrow = true
            testWhileIdle = true
        }

    }
    private val logger = LoggerFactory.getLogger(RedisProxy::class.java.simpleName)

//    private val jedis = Jedis(redisName, redisPort, DefaultJedisClientConfig.builder()
//        .password(redisKey)
//        .ssl(true)
//        .timeoutMillis(400000)
//        .build()
//
//    )

    private val jedisPool = JedisPool(jedisPoolConfig, redisName,redisPort, 400000, redisKey,true)
    init {
        logger.info("REDIS connection established with $redisName")
    }

    fun getJedisClient(): Jedis {
        return jedisPool.resource
    }

    fun close() {
        jedisPool.close()
    }
}