package gov.cdc.dex.mrr

import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis


class RedisUtility {
    @Throws(Exception::class)
    fun redisConnection(): Jedis {
        val redisCacheName = System.getenv("REDISCACHEHOSTNAME")
        val rediscachekey = System.getenv("REDISCACHEKEY")
        var jedis = Jedis()

         println("cacheHostname :\${redisCacheName} ")
        try {
            // Connect to the Azure Cache for Redis over the TLS/SSL port using the key.
              jedis = Jedis(
                redisCacheName, 6380, DefaultJedisClientConfig.builder()
                    .password(rediscachekey)
                    .ssl(true)
                    .build()
            )

        } catch (e: Exception) {
            throw Exception("Radis Connection Failure: ${e.printStackTrace()}")
        }
        return jedis;
    }
}