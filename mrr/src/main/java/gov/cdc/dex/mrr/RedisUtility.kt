package gov.cdc.dex.mrr


import gov.cdc.dex.azure.RedisProxy
import redis.clients.jedis.Jedis


class RedisUtility {
    @Throws(Exception::class)
    fun redisConnection(): Jedis {
        val redisCacheName = System.getenv("REDIS_CACHE_NAME")
        val rediscachekey = System.getenv("REDIS_CACHE_KEY")
        val jedis: Jedis
        try {
           jedis =RedisProxy(redisCacheName,rediscachekey).getJedisClient()
        } catch (e: Exception) {
            throw Exception("Redis Connection Failure: ${e.printStackTrace()}")
        }
        return jedis
    }
}