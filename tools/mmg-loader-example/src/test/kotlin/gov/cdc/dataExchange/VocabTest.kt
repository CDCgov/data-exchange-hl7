package gov.cdc.dataExchange


import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import open.HL7PET.tools.HL7StaticParser
import org.junit.jupiter.api.Test
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis
import scala.Enumeration.Value
import kotlin.test.DefaultAsserter.assertTrue

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

        val listType = object : TypeToken<List<ValueSetConcept>>() {}.type
        val concepts:List<ValueSetConcept> = Gson().fromJson(conceptStr, listType)
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
    @Test
    fun testInvalidKey() {
        val mmgValidator = MMGValidator()
        try {
            val unknownKey = mmgValidator.retrieveValueSetConcepts("Unknown_key")
            println(unknownKey)
        } catch (e: InvalidConceptKey) {
            assertTrue("Exception properly thrown", true)
        }
    }

    @Test
    fun testMultipleKeys() {
        val result = jedis.mget("PHVS_YesNoUnkNA_NND", "unknwon",  "PHVS_ReasonForHospitalization_VZ")
        println(result)
    }

    @Test
    fun testGetFirtValue() {
        val msg = this::class.java.getResource("/testMessage.hl7").readText()
        val msgValue = HL7StaticParser.getFirstValue(msg, "MSH-21[3].1")
        if (msgValue.isDefined)
            println(msgValue.get())
        else
            println("No value found")
    }

}