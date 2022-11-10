package gov.cdc.dex.mrr


import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis


class RedisUtility {
    @Throws(Exception::class)
    fun redisConnection(): Jedis {
        val redisCacheName = System.getenv("REDIS_CACHE_NAME")
        val rediscachekey = System.getenv("REDIS_CACHE_KEY")
        val jedis: Jedis

        println("cacheHostname :${redisCacheName} ")
        try {
            // Connect to the Azure Cache for Redis over the TLS/SSL port using the key.
              jedis = Jedis(
                redisCacheName, 6380, DefaultJedisClientConfig.builder()
                    .password(rediscachekey)
                    .ssl(true)
                    .timeoutMillis(300000)
                    .build()
            )
        } catch (e: Exception) {
            throw Exception("Redis Connection Failure: ${e.printStackTrace()}")
        }
        return jedis
    }
}