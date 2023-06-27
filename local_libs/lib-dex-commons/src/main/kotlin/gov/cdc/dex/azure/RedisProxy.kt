package gov.cdc.dex.azure

import gov.cdc.dex.util.StringUtils.Companion.getOrDefault
import org.slf4j.LoggerFactory
import redis.clients.jedis.*
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
class RedisProxy(redisName: String, redisKey:String, redisPort: Int = 6380, ssl: Boolean = true)  {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisProxy::class.java.simpleName)
        const val REDIS_CACHE_NAME_PROP_NAME: String = "REDIS_CACHE_NAME"
        const val REDIS_PWD_PROP_NAME: String = "REDIS_CACHE_KEY"
        const val REDIS_PORT_PROP_NAME: String = "REDIS_PORT"
    }

    val jedisPool:JedisPool

    init {
        val jedisPoolConfig = JedisPoolConfig().apply {
            maxTotal =       System.getenv("REDIS_POOL_MAX_TOTAL").getOrDefault("50").toInt()
            maxIdle =        System.getenv("REDIS_POOL_MAX_IDLE").getOrDefault("50").toInt()
            minIdle =        System.getenv("REDIS_POOL_MIN_IDLE").getOrDefault("5").toInt()
            testOnBorrow =   System.getenv("REDIS_POOL_TEST_ON_BORROW").getOrDefault("false").toBoolean()
            testWhileIdle =  System.getenv("REDIS_POOL_TEST_WHILE_IDLE").getOrDefault("false").toBoolean()
            this.testOnCreate = false
            this.testOnReturn = false

        }
        val timeout = System.getenv("REDIS_POOL_TIMEOUT").getOrDefault("300000").toInt()
        logger.info("DEX::Attempting REDIS connection pool instantiation with $redisName at ${LocalDateTime.now()}. . .")
        jedisPool = JedisPool(jedisPoolConfig, redisName, redisPort, timeout, redisKey, ssl)
        logger.info("DEX::Updated! REDIS connection established with $redisName at  at ${LocalDateTime.now()}")
        val poolTime = measureTime {
            jedisPool.preparePool()
        }

        logger.info("DEX::Jedis Pool prepared at ${LocalDateTime.now()}")
        logger.info("DEX::Time to create pool: $poolTime")
    }

    fun getJedisClient(): Jedis {
        logger.info("DEX::Supplying Redis connection pool resource")
        return jedisPool.resource
    }

    fun releaseJedisClient(conn: Jedis) {
        logger.info("DEX::Releasing Redis connection pool resource")
        conn.close()
    }
}