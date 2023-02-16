
import gov.cdc.dex.hl7.MmgUtil
import gov.cdc.hl7.HL7StaticParser

import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertFails

import org.slf4j.LoggerFactory
import java.util.*

import gov.cdc.dex.redisModels.MMG

import gov.cdc.dex.redisModels.Condition2MMGMapping 

import gov.cdc.dex.hl7.model.PhinDataType
import gov.cdc.dex.redisModels.ValueSetConcept

import gov.cdc.dex.hl7.Transformer

import gov.cdc.dex.mmg.InvalidConditionException

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.GsonBuilder

import  gov.cdc.dex.azure.RedisProxy

class MbtTest {

    companion object {

        val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")
        
        val redisProxy = RedisProxy(REDIS_CACHE_NAME, REDIS_PWD)
        val redisClient = redisProxy.getJedisClient()

        val REDIS_PREFIX_COONDITION = "condition:"

        val logger = LoggerFactory.getLogger(MmgUtil::class.java.simpleName)
        private val gson = Gson()
        private val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() 

    } // .companion 


    @Test
    fun testRedisInstanceUsed() {

        logger.info("testRedisInstanceUsed: REDIS_CACHE_NAME: --> ${REDIS_CACHE_NAME}")
        assertEquals(REDIS_CACHE_NAME, "tf-vocab-cache-dev.redis.cache.windows.net")
    } // .testRedisInstanceUsed


    @Test
    fun testLocalMMGToClass() {

        val mmgPath = "/Generic Version 2.0.1.json"
        val mmgJson = this::class.java.getResource(mmgPath).readText()

        val mmg = gson.fromJson(mmgJson, MMG::class.java)
        logger.info("testLocalMMGToClass: mmg.name: ${mmg.name}, mmg.blocks.size: ${mmg.blocks.size}")

        assertEquals(mmg.name, "Generic Version 2.0.1")
        assertEquals(mmg.blocks.size, 8)
    } // .testLoadMMG


    @Test
    fun testRedisReadGenericMmg() {

        val mmg = redisClient.get("mmg:generic_mmg_v2_0").substring(0, 100)
        logger.info("testRedisReadGenericMmg: mmg: ${mmg}...")

        assertEquals(mmg.length, 100)
    } // .testLoadMMG


    @Test
    fun testRedisReadTBRDMmg() {

        val mmg = redisClient.get("mmg:tbrd").substring(0, 100)
        logger.info("testRedisReadTBRDMmg: mmg: ${mmg}...")

        assertEquals(mmg.length, 100)
    } // .testLoadMMG


    @Test
    fun testGetMMGFromMessageTbrd() {
        
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val hl7Content = this::class.java.getResource(filePath).readText()

        val mmgUtil = MmgUtil(redisProxy)
        val mmgs = mmgUtil.getMMGFromMessage(hl7Content, filePath, "messageUUID")

        logger.info("testGetMMGFromMessage: for filePath: $filePath, mmgs.size: --> ${mmgs.size}")

        mmgs.forEach {
            logger.info("testGetMMGFromMessage: MMG for filePath: $filePath, MMG name: --> ${it.name}, MMG BLOCKS: --> ${it.blocks.size}")
        }
        
        assertEquals(mmgs.size, 2)
    } // .testLoadMMG


    @Test
    fun testGetRedisCondition2MMGMapping() {

        val ccJson = redisClient.get( REDIS_PREFIX_COONDITION + "11088" )
        logger.info("Redis JSON: --> $ccJson")
        assertTrue(ccJson.length > 0 )

        val cc = gson.fromJson(ccJson, Condition2MMGMapping::class.java)

        logger.info("Redis condition code entry: --> $cc")
        assertEquals(cc.profiles!!.size, 1)
    } // .testLoadMMG


    @Test
    fun testLoadMMGfromMessage() {

        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgUtil = MmgUtil(redisProxy)
        val mmgsArr = mmgUtil.getMMGFromMessage(testMsg, filePath, "")

        val transfomer = Transformer(redisProxy)
        val mmgs = transfomer.getMmgsFiltered(mmgsArr)

        mmgs.forEach {
            logger.info("MMG ID: ${it.id}, NAME: ${it.name}, BLOCKS: --> ${it.blocks.size}")
        }

        assertEquals(mmgs.size, 2)
        assertEquals(mmgs[0].blocks.size + mmgs[1].blocks.size, 7 + 26) // the Message Header is trimmed from the GenV2 hence 7 and not 8
    } // .testLoadMMGfromMessage


    @Test
    fun testRedisMMGToClass() {

        val mmg = gson.fromJson(redisClient.get("mmg:tbrd"), MMG::class.java)
        logger.info("testRedisMMGToClass: MMG name: ${mmg.name}, blocks: ${mmg.blocks.size}")

        assertEquals(mmg.name, "TBRD")
        assertEquals(mmg.blocks.size, 26)
    } // .testLoadMMG


    @Test
    fun testPhinDataTypesToMapOfListClass() {
        val transformer = Transformer(redisProxy)

        val dataTypesMap: Map<String, List<PhinDataType>> = transformer.getPhinDataTypes()

        logger.info("testPhinDataTypesToMapOfListClass: Phin dataTypesMap.size: --> ${dataTypesMap.size}")
        // Phin dataTypesMap.size: --> 12
        assertEquals(dataTypesMap.size, 12)
    } // .testPhinDataTypes


    @Test
    fun testTransformerHl7ToJsonModelTC01() {

        // mmg1
        val mmg1Path = "/Generic Version 2.0.1.json"
        val mmg1Json = this::class.java.getResource(mmg1Path).readText()
        val mmg1 = gson.fromJson(mmg1Json, MMG::class.java)

        // mmg2 TODO:
        val mmg2Path = "/TBRD.json"
        val mmg2Json = this::class.java.getResource(mmg2Path).readText()
        val mmg2 = gson.fromJson(mmg2Json, MMG::class.java)

        val mmgs = arrayOf(mmg1, mmg2)

        // hl7
        val hl7FilePath = "/TBRD_V1.0.2_TM_TC01.hl7" // "/Genv2_2-0-1_TC01.hl7" // "/TBRD_V1.0.2_TM_TC01.hl7"
        val hl7Content = this::class.java.getResource(hl7FilePath).readText()
        
        val transformer = Transformer(redisProxy)
        val model1 = transformer.hl7ToJsonModelBlocksSingle(hl7Content, mmgs)

        val model2 = transformer.hl7ToJsonModelBlocksNonSingle(hl7Content, mmgs)

        val model = model1 + model2

        logger.info("testTransformerHl7ToJsonModel: MMG model.size: ${model.size}")
        // MMG model.size: 89
        assertEquals(model.size, 89)
    } // .testTransformerHl7ToJsonModel
 

    @Test
    fun testTransformerHl7ToJsonModelwithRedisMmgTC04() {
        
        // hl7
        val hl7FilePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val hl7Content = this::class.java.getResource(hl7FilePath).readText()

        val mmgUtil = MmgUtil(redisProxy)
        val mmgs = mmgUtil.getMMGFromMessage(hl7Content, hl7FilePath, "")

        mmgs.forEach {
            logger.info("MMG ID: ${it.id}, NAME: ${it.name}, BLOCKS: --> ${it.blocks.size}")
        }

        val transformer = Transformer(redisProxy)
        val model1 = transformer.hl7ToJsonModelBlocksSingle(hl7Content, mmgs)

        val model2 = transformer.hl7ToJsonModelBlocksNonSingle(hl7Content, mmgs)

        val mmgModel = model1 + model2

        logger.info("testTransformerHl7ToJsonModelwithRedisMmgTC04: MMG Model (mmgModel): --> \n\n${gsonWithNullsOn.toJson(mmgModel)}\n")
    } // .testTransformerHl7ToJsonModelwithRedisMmg


    @Test
    fun testConditionNotSupportedException() {

        assertFails(

            block = {

                val filePath = "/Genv2_2-0-1_TC01.hl7"
                val hl7Content = this::class.java.getResource(filePath).readText()
        
                val mmgUtil = MmgUtil(redisProxy)
                val mmgs = mmgUtil.getMMGFromMessage(hl7Content, filePath, "messageUUID")
        
                logger.info("testConditionNotSupportedException: for filePath: $filePath, mmgs.size: --> ${mmgs.size}")
        
                mmgs.forEach {
                    logger.info("testConditionNotSupportedException: MMG for filePath: $filePath, MMG name: --> ${it.name}, MMG BLOCKS: --> ${it.blocks.size}")
                }

            } // .block

        ) // .assertFails
    
        // TODO: If this was supported
        // assertEquals(mmgs.size, 1)
        // assertEquals(mmgs[0].name, "Generic Version 2.0.1")

    } // .testLoadMMG


    fun testMmgThrowsException() {

        assertFailsWith<InvalidConditionException>(

            block = {

                val mmgUtil = MmgUtil(redisProxy)
                val mmgs = mmgUtil.getMMGFromMessage("hl7Content", "hl7FilePath", "")
                logger.info("testMmgThrowsException: mmgs.size: ${mmgs.size}")

            } // .block

        ) // .assertFailsWith

    } // .testMmgThrowsException


} // .MbtTest

