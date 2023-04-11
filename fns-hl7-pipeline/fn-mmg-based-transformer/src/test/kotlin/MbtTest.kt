
import gov.cdc.hl7.HL7StaticParser

import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertFails

import org.slf4j.LoggerFactory

import gov.cdc.dex.redisModels.MMG

import gov.cdc.dex.redisModels.Condition2MMGMapping 

import gov.cdc.dex.hl7.model.PhinDataType

import gov.cdc.dex.hl7.Transformer

import gov.cdc.dex.mmg.InvalidConditionException

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import  gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.metadata.DexMessageInfo
import gov.cdc.dex.metadata.HL7MessageType
import gov.cdc.dex.mmg.MmgUtil
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.StringUtils
import gov.cdc.dex.util.StringUtils.Companion.normalize


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

        const val JURISDICTION_CODE_PATH = "OBX[@3.1='77966-0']-5.1"
        const val EVENT_CODE_PATH = "OBR-31.1"

    } // .companion 


    @Test
    fun testRedisInstanceUsed() {

        logger.info("testRedisInstanceUsed: REDIS_CACHE_NAME: --> ${REDIS_CACHE_NAME}")
        assertEquals(REDIS_CACHE_NAME, "ocio-ede-dev-dex-cache.redis.cache.windows.net")
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
        val reportingJurisdiction = extractValue(hl7Content, JURISDICTION_CODE_PATH)
        val eventCode = extractValue(hl7Content, EVENT_CODE_PATH)
        val mmgs = getMMGsFromMessage(hl7Content, reportingJurisdiction, eventCode)

        logger.info("testGetMMGsFromMessage: for filePath: $filePath, mmgs.size: --> ${mmgs.size}")

        mmgs.forEach {
            logger.info("testGetMMGsFromMessage: MMG for filePath: $filePath, MMG name: --> ${it.name}, MMG BLOCKS: --> ${it.blocks.size}")
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
    fun testLoadMMGsFromKeyList() {
        val dmi = DexMessageInfo("10030", "some route", listOf("mmg:varicella_message_mapping_guide_v2_01"),
            "13", HL7MessageType.CASE)
        val gson = Gson()
        val dmiString = gson.toJson(dmi)
        val jsonObj = JsonParser.parseString(dmiString) as JsonObject
        println(jsonObj["mmgs"])
        println(jsonObj["mmgs"].javaClass.name)
        val mmgKeys = JsonHelper.getStringArrayFromJsonArray(jsonObj["mmgs"].asJsonArray)

        val mmgUtil = MmgUtil(redisProxy)
        val mmgs = mmgUtil.getMMGs(mmgKeys)
        assert(mmgs.size == 1)
    }
    @Test
    fun testLoadMMGfromMessage() {

        val filePath = "/Varicella_AK_2021_1.txt"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgUtil = MmgUtil(redisProxy)
        val mmgsArr = mmgUtil.getMMGs("Var_Case_Map_v2.0", "", "10030", "13")

        val transformer = Transformer(redisProxy)
        val mmgs = transformer.getMmgsFiltered(mmgsArr)

        mmgs.forEach {
            logger.info("MMG ID: ${it.id}, NAME: ${it.name}, BLOCKS: --> ${it.blocks.size}")
        }

        assertEquals(mmgs.size, 1)
       // assertEquals(mmgs[0].blocks.size + mmgs[1].blocks.size, 7 + 26) // the Message Header is trimmed from the GenV2 hence 7 and not 8
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
    fun getSmallBlockName(){
        val name = "Vaccination History Section to specify the detailed vaccine record information - Repeats for each vaccine dose."
        val blockName = if (name.normalize().contains("repeating_group")) {
            name
        } else {
            "$name repeating group"
        }
        val smallName = StringUtils.getNormalizedShortName(blockName, 30)
        println(smallName)
        assert(smallName.length <= 30)
        assert(smallName.endsWith("_rg"))
    }
    @Test
    fun testTransformerHl7ToJsonModelwithRedisMmgTC04() {
        testTransformerWithRedis("testTC04", "/TBRD_V1.0.2_TM_TC04.hl7")

    } // .testTransformerHl7ToJsonModelwithRedisMmg

    @Test
    fun testTransformerHep() {
       testTransformerWithRedis("testHep", "/KY_Hepatitis Round 2_TM4.txt")
    } // .testTransformerHep

    @Test
    fun testTransformerGenv1() {

        testTransformerWithRedis("testGenv1", "/Tuleremia.hl7")
    }

    @Test
    fun testMumps() {
        testTransformerWithRedis("testMumps", "/MUMPS_V1-0-1_TM_TC01.txt")

    }

    @Test
    fun testPertussis() {
        testTransformerWithRedis("testPertussis", "/PERT_V1.0.1_TM_TC01.txt")
    }

    @Test
    fun testTuberculosis() {
        testTransformerWithRedis("testTuberculosis", "/TB and LTBI_3-0-3_TC01.txt")
    }

    @Test
    fun testMalaria() {
        testTransformerWithRedis("testMalaria", "/Malaria_V1.0.2__TC08.txt")
    }
    @Test
    fun testConditionNotSupportedException() {

        assertFails(

            block = {

                val filePath = "/Genv2_2-0-1_TC01.hl7"
                val hl7Content = this::class.java.getResource(filePath).readText()

                val reportingJurisdiction = extractValue(hl7Content, JURISDICTION_CODE_PATH)
        
                val mmgs = getMMGsFromMessage(hl7Content, reportingJurisdiction, "11550")
        
                logger.info("testConditionNotSupportedException: for filePath: $filePath, mmgs.size: --> ${mmgs.size}")
        
                mmgs.forEach {
                    logger.info("testConditionNotSupportedException: MMG for filePath: $filePath, MMG name: --> ${it.name}, MMG BLOCKS: --> ${it.blocks.size}")
                }

            } // .block

        ) // .assertFails


    } // .testLoadMMG

    @Test
    fun testMmgThrowsException() {

        assertFailsWith<InvalidConditionException>(

            block = {

                val mmgs = getMMGsFromMessage("hl7Content", "23", "88888")
                logger.info("testMmgThrowsException: mmgs.size: ${mmgs.size}")

            } // .block

        ) // .assertFailsWith

    } // .testMmgThrowsException


    private fun extractValue(msg: String, path: String): String  {
        val value = HL7StaticParser.getFirstValue(msg, path)
        return if (value.isDefined) value.get()
        else ""
    }

    private fun getMMGsFromMessage(messageContent: String, jurisdictionCode: String, eventCode: String) : Array<MMG>{
        val mshProfile= extractValue(messageContent, "MSH-21[2].1")
        val mshCondition = extractValue(messageContent, "MSH-21[3].1")
      //  val eventCode = extractValue(messageContent, "OBR-31.1")
       // val jurisdictionCode = "13"
        val mmgUtil = MmgUtil(redisProxy)
        return mmgUtil.getMMGs(mshProfile, mshCondition, eventCode, jurisdictionCode)
    }

    private fun testTransformerWithRedis(testName: String, filePath: String) {

        val hl7Content = this::class.java.getResource(filePath).readText()
        val reportingJurisdiction = extractValue(hl7Content, JURISDICTION_CODE_PATH)
        val eventCode = extractValue(hl7Content, EVENT_CODE_PATH)
        val mmgs = getMMGsFromMessage(hl7Content, reportingJurisdiction, eventCode)

        mmgs.forEach {
            logger.info("MMG ID: ${it.id}, NAME: ${it.name}, BLOCKS: --> ${it.blocks.size}")
        }

        val transformer = Transformer(redisProxy)
        val model1 = transformer.hl7ToJsonModelBlocksSingle(hl7Content, mmgs)

        val model2 = transformer.hl7ToJsonModelBlocksNonSingle(hl7Content, mmgs)

        val mmgModel = model1 + model2
        logger.info("$testName: MMG Model (mmgModel): --> \n\n${gsonWithNullsOn.toJson(mmgModel)}\n")
    }
} // .MbtTest

