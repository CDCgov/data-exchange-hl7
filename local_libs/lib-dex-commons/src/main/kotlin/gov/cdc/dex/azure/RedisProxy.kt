package gov.cdc.dex.azure

import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class RedisProxy( redisName: String,  redisKey:String,  redisPort: Int = 6380, ssl: Boolean = true)  {
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

    private val jedisPool = JedisPool(jedisPoolConfig, redisName,redisPort, 400000, redisKey,ssl)


    init {
        logger.info("REDIS connection established with $redisName")
    }

    fun getJedisClient(): Jedis {
        return jedisPool.resource
    }

    fun releaseJedisClient(conn: Jedis) {
        conn.close()
    }
}