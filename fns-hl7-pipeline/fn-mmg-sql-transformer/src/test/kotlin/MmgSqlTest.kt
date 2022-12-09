// import gov.cdc.dex.hl7.MmgUtil
// import gov.cdc.hl7.HL7StaticParser

import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
// import kotlin.test.assertTrue

import org.slf4j.LoggerFactory
// import java.util.*


// import gov.cdc.dex.redisModels.MMG
// import gov.cdc.dex.hl7.model.ConditionCode
// import gov.cdc.dex.hl7.model.PhinDataType
// import gov.cdc.dex.redisModels.ValueSetConcept


// import com.google.gson.Gson
// import com.google.gson.reflect.TypeToken
// import com.google.gson.GsonBuilder

// import  gov.cdc.dex.azure.RedisProxy

class MmgSqlTest {

    companion object {

        val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        // val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")
        
        // val redisProxy = RedisProxy(REDIS_CACHE_NAME, REDIS_PWD)

        // val redisClient = redisProxy.getJedisClient()

        val logger = LoggerFactory.getLogger(MmgSqlTest::class.java.simpleName)
        // private val gson = Gson()
        // private val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() 
    } // .companion 

    @Test
    fun testRedisInstanceUsed() {

        logger.info("testRedisInstanceUsed: REDIS_CACHE_NAME: --> ${REDIS_CACHE_NAME}")
        assertEquals(REDIS_CACHE_NAME, "tf-vocab-cache-dev.redis.cache.windows.net")
    } // .testRedisInstanceUsed


} // .MmgSqlTest



