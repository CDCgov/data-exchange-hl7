package gov.cdc.dex.azure

import gov.cdc.dex.util.StringUtils.Companion.getOrDefault
import org.slf4j.LoggerFactory
import redis.clients.jedis.*

class RedisProxy( redisName: String,  redisKey:String,  redisPort: Int = 6380, ssl: Boolean = true)  {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisProxy::class.java.simpleName)

        const val REDIS_CACHE_NAME_PROP_NAME: String = "REDIS_CACHE_NAME"
        const val REDIS_PWD_PROP_NAME: String = "REDIS_CACHE_KEY"
        const val REDIS_PORT_PROP_NAME: String = "REDIS_PORT"



    }

     val jedisPool:JedisPool

    init {
        logger.info("REDIS connection established with $redisName")
        val jedisPoolConfig = JedisPoolConfig().apply {
            maxTotal =       System.getenv("REDIS_POOL_MAX_TOTAL").getOrDefault("300").toInt()
            maxIdle =        System.getenv("REDIS_POOL_MAX_IDLE").getOrDefault("300").toInt()
            minIdle =        System.getenv("REDIS_POOL_MIN_IDLE").getOrDefault("20").toInt()
            testOnBorrow =   System.getenv("REDIS_POOL_TEST_ON_BORROW").getOrDefault("true").toBoolean()
            testWhileIdle =  System.getenv("REDIS_POOL_TEST_WHILE_IDLE").getOrDefault("true").toBoolean()
            this.testOnCreate = false
            this.testOnReturn = false

        }
        val timeout = System.getenv("REDIS_POOL_TIMEOUT").getOrDefault("4000").toInt()

        jedisPool = JedisPool(jedisPoolConfig, redisName,redisPort, timeout, redisKey,ssl)

        jedisPool.preparePool()
    }

    fun getJedisClient(): Jedis {
        return jedisPool.resource
    }

    fun releaseJedisClient(conn: Jedis) {
        conn.close()
    }
}