package gov.cdc.dex.mrr

import gov.cdc.dex.azure.RedisProxy
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

internal class RedisUtilityTest {

    @Test
    fun testAddConditon() {
        try {
            val key = "conditionv2:10049"
            val value = "{\"event_code\":10049,\"name\":\"West Nile virus non-neuroinvasive disease\",\"program\":\"NCEZID\",\"category\":\"Arboviral Diseases\",\"profiles\":[{\"name\":\"arbo_case_map_v1.0\",\"mmgs\":[\"mmg:arboviral_v1_3_2_mmg_20210721\"],\"special_cases\":[{\"applies_to\":\"group:legacy_arbo\",\"mmg_maps\":[\"mmg:arboviral_human_case_message_mapping_guide\"]}]}]}"
            val jedis = RedisUtility().redisConnection()
            val pipeline = jedis.pipelined()
            pipeline.set(key, value)
            pipeline.sync()
            pipeline.close()
        } catch (e: Exception) {
            println("Problem connecting to Redis:${e.printStackTrace()}")
        }
    }

    @Test
    fun testReadCondition() {
        try {
            val key = "conditionv2:11088"
            val redisName =  System.getenv("REDIS_CACHE_NAME")
            val redisKey = System.getenv("REDIS_CACHE_KEY")
            val redisProxy = RedisProxy(redisName, redisKey)
            redisProxy.getJedisClient().use {
                val cond = it.get(key)
                println(cond)
            }

        } catch (e: Exception) {
            println("Problem connecting to Redis:${e.printStackTrace()}")


        }
    }

    @Test
    fun testPing() {
//        val redisName = "tf-vocab-cache-dev.redis.cache.windows.net"
//        val jedis = Jedis(redisName, 6380, DefaultJedisClientConfig.builder()
//            .password("unvalid")
//            .ssl(true)
//            .timeoutMillis(400000)
//            .build())
//
//        jedis.ping()
    }

}