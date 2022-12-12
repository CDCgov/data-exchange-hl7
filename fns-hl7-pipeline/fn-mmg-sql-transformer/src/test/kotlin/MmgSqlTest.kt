import gov.cdc.dex.hl7.MmgUtil
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

import com.google.gson.Gson
// import com.google.gson.reflect.TypeToken
import com.google.gson.GsonBuilder

import  gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.TransformerSql

class MmgSqlTest {

    companion object {
        val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(REDIS_CACHE_NAME, REDIS_PWD)
        // val redisClient = redisProxy.getJedisClient()
        val logger = LoggerFactory.getLogger(MmgSqlTest::class.java.simpleName)
        private val gson = Gson()
        private val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() 
    } // .companion 

    // @Test
    // fun testRedisInstanceUsed() {
    //     logger.info("testRedisInstanceUsed: REDIS_CACHE_NAME: --> ${REDIS_CACHE_NAME}")
    //     assertEquals(REDIS_CACHE_NAME, "tf-vocab-cache-dev.redis.cache.windows.net")
    // } // .testRedisInstanceUsed


    @Test
    fun testTransformerSql() {

        // MMGs for the message
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgUtil = MmgUtil(redisProxy)
        val mmgs = mmgUtil.getMMGFromMessage(testMsg, filePath, "")
        assertEquals(mmgs.size, 2)

        // MMG Based Model for the message
        val mmgBasedModelPath = "/model.json"
        val mmgBasedModelStr = this::class.java.getResource(mmgBasedModelPath).readText()
        // logger.info("mmgBasedModelStr: --> ${mmgBasedModelStr}")

        val transformer = TransformerSql()
        val mmgSqlModel = transformer.toSqlModel(mmgs, mmgBasedModelStr)
        logger.info(" mmgSqlModel: --> ${gsonWithNullsOn.toJson(mmgSqlModel)}")

    } // .testRedisInstanceUsed


} // .MmgSqlTest



