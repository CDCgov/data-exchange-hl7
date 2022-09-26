package gov.cdc.dataExchange

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

class VocabTest {


    val REDIS_PWD = System.getenv("REDISCACHEKEY")

    val jedis = Jedis(MMGValidator.REDIS_CACHE_NAME, 6380, DefaultJedisClientConfig.builder()
        .password(REDIS_PWD)
        .ssl(true)
        .build()
    )

    @Test
    fun testLoadVocab() {
        val conceptStr = jedis.get("PHVS_County_FIPS_6-4")
        val mapper = jacksonObjectMapper()
        val concepts = mapper.readValue(conceptStr, List::class.java)
        println(concepts)
    }
    @Test
    fun testLoadVocabFromMMGValidator() {
        val mmgValidator = MMGValidator()
        val concepts = mmgValidator.retrieveValueSetConcepts("PHVS_State_FIPS_5-2")
        println(concepts)
    }

    @Test
    fun testGetRedisKeys() {
        val keys = jedis.keys("*")
        println(keys)
    }
}