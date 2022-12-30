import gov.cdc.dex.hl7.MmgUtil

import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import gov.cdc.dex.mmg.InvalidConditionException
import kotlin.test.assertFails

import org.slf4j.LoggerFactory

import gov.cdc.dex.hl7.model.PhinDataType

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonParser

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

        const val MMG_BLOCK_TYPE_SINGLE = "Single"
        const val TABLES_KEY_NAME = "tables"

        const val REDIS_INSTANCE_NAME = "tf-vocab-cache-dev.redis.cache.windows.net"

        val MESSAGE_PROFILE_IDENTIFIER = "message_profile_identifier"
    } // .companion 


    @Test
    fun testRedisInstanceUsed() {
        logger.info("testRedisInstanceUsed: REDIS_CACHE_NAME: --> ${REDIS_CACHE_NAME}")
        assertEquals(REDIS_CACHE_NAME, REDIS_INSTANCE_NAME)
    } // .testRedisInstanceUsed


    @Test
    fun testMMGUtil() {

        // MMGs for the message
        // ------------------------------------------------------------------------------
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgUtil = MmgUtil(redisProxy)
        val mmgsArr = mmgUtil.getMMGFromMessage(testMsg, filePath, "")
        logger.info("mmgsArr.size: --> ${mmgsArr.size}")

        assertEquals(mmgsArr.size, 2)
    } // .testMMGUtil


    @Test
    fun testDefaultPhinProfiles() {
        // Default Phin Profiles Types
        // ------------------------------------------------------------------------------
        val dataTypesFilePath = "/DefaultFieldsProfileX.json"
        val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath).readText()
        val dataTypesMapType = object : TypeToken< Map<String, List<PhinDataType>> >() {}.type
        val profilesMap: Map<String, List<PhinDataType>> = gson.fromJson(dataTypesMapJson, dataTypesMapType)
        logger.info("profilesMap.size: --> ${profilesMap.size}")

        assertEquals(profilesMap.size, 12)
    } // .testDefaultPhinProfiles


    @Test
    fun testTransformerSQLForTC04() {

        // MMGs for the message
        // ------------------------------------------------------------------------------
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgUtil = MmgUtil(redisProxy)
        val mmgsArr = mmgUtil.getMMGFromMessage(testMsg, filePath, "")

        // Default Phin Profiles Types
        // ------------------------------------------------------------------------------
        val dataTypesFilePath = "/DefaultFieldsProfileX.json"
        val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath).readText()
        val dataTypesMapType = object : TypeToken< Map<String, List<PhinDataType>> >() {}.type
        val profilesMap: Map<String, List<PhinDataType>> = gson.fromJson(dataTypesMapJson, dataTypesMapType)


        // MMG Based Model for the message
        // ------------------------------------------------------------------------------
        val mmgBasedModelPath = "/mmgBasedModel1.json"
        val mmgBasedModelStr = this::class.java.getResource(mmgBasedModelPath).readText()
        val modelJson = JsonParser.parseString(mmgBasedModelStr).asJsonObject

        // logger.info("mmgBasedModelStr: --> ${mmgBasedModelStr}")


        // Transformer SQL
        // ------------------------------------------------------------------------------
        val transformer = TransformerSql()

        val mmgs = transformer.getMmgsFiltered(mmgsArr)
        val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks
        val (mmgBlocksSingle, mmgBlocksNonSingle) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }
        val ( mmgElementsSingleNonRepets, mmgElementsSingleRepeats ) = mmgBlocksSingle.flatMap { it.elements }.partition{ !it.isRepeat }

        // Singles Non Repeats
        // --------------------------------------
        val singlesNonRepeatsModel = transformer.singlesNonRepeatsToSqlModel(mmgElementsSingleNonRepets, profilesMap, modelJson)

        // logger.info("singlesNonRepeatsModel: -->\n\n${gsonWithNullsOn.toJson(singlesNonRepeatsModel)}\n")  
        logger.info("singlesNonRepeatsModel.size: --> ${singlesNonRepeatsModel.size}")     
        assertEquals(singlesNonRepeatsModel.size, 172)

        // Singles Repeats
        // --------------------------------------
        val singlesRepeatsModel = transformer.singlesRepeatsToSqlModel(mmgElementsSingleRepeats, profilesMap, modelJson)

        // logger.info("singlesRepeatsModel: -->\n\n${gsonWithNullsOn.toJson(singlesRepeatsModel)}\n") 
        logger.info("singlesRepeatsModel.size: --> ${singlesRepeatsModel.size}") 
        assertEquals(singlesRepeatsModel.size, 5) // original 6, 5 after message profile identifier is filtered out and moving to singles non repeats
        
 
        // Repeated Blocks
        // --------------------------------------
        val repeatedBlocksModel = transformer.repeatedBlocksToSqlModel(mmgBlocksNonSingle, profilesMap, modelJson)

        // logger.info("repeatedBlocksModel: -->\n\n${gsonWithNullsOn.toJson(repeatedBlocksModel)}\n")   
        logger.info("repeatedBlocksModel.size: --> ${repeatedBlocksModel.size}")  
        assertEquals(repeatedBlocksModel.size, 10)

        
        // Message Profile Identifier
        val mesageProfIdModel = transformer.messageProfIdToSqlModel(modelJson)
        logger.info("mesageProfIdModel.size: --> ${mesageProfIdModel.size}")  
        assertEquals(mesageProfIdModel.size, 3)

        val mmgSqlModel = mesageProfIdModel + singlesNonRepeatsModel + mapOf(
            TABLES_KEY_NAME to singlesRepeatsModel + repeatedBlocksModel,
        ) // .mmgSqlModel

        logger.info("mmgSqlModel.size: --> ${mmgSqlModel.size}")  
        assertEquals(mesageProfIdModel.size + singlesNonRepeatsModel.size + 1, 3 + 172 + 1 )  // 1 for tables


        logger.info("mmgSqlModel: -->\n\n${gsonWithNullsOn.toJson(mmgSqlModel)}\n")   
    } // .testTransformerSQLForTC04

    @Test
    fun testMessageProfileIdentifier() {

        // MMGs for the message
        // ------------------------------------------------------------------------------
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgUtil = MmgUtil(redisProxy)
        val mmgsArr = mmgUtil.getMMGFromMessage(testMsg, filePath, "")

        // Default Phin Profiles Types
        // ------------------------------------------------------------------------------
        val dataTypesFilePath = "/DefaultFieldsProfileX.json"
        val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath).readText()
        val dataTypesMapType = object : TypeToken< Map<String, List<PhinDataType>> >() {}.type
        val profilesMap: Map<String, List<PhinDataType>> = gson.fromJson(dataTypesMapJson, dataTypesMapType)


        // MMG Based Model for the message
        // ------------------------------------------------------------------------------
        val mmgBasedModelPath = "/mmgBasedModel1.json"
        val mmgBasedModelStr = this::class.java.getResource(mmgBasedModelPath).readText()
        val modelJson = JsonParser.parseString(mmgBasedModelStr).asJsonObject

        // logger.info("mmgBasedModelStr: --> ${mmgBasedModelStr}")


        // Transformer SQL
        // ------------------------------------------------------------------------------
        val transformer = TransformerSql()

        val mmgs = transformer.getMmgsFiltered(mmgsArr)
        val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks
        val (mmgBlocksSingle, _) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }
        val ( mmgElementsSingleNonRepets, mmgElementsSingleRepeats ) = mmgBlocksSingle.flatMap { it.elements }.partition{ !it.isRepeat }

        // Message Profile Identifier
        val mesageProfIdModel = transformer.messageProfIdToSqlModel(modelJson)

        // Singles Non Repeats
        // --------------------------------------
        val singlesNonRepeatsModel = mesageProfIdModel + transformer.singlesNonRepeatsToSqlModel(mmgElementsSingleNonRepets, profilesMap, modelJson)

        logger.info("singlesNonRepeatsModel[$MESSAGE_PROFILE_IDENTIFIER]: --> ${singlesNonRepeatsModel[MESSAGE_PROFILE_IDENTIFIER]}")
        assertEquals(singlesNonRepeatsModel.contains(MESSAGE_PROFILE_IDENTIFIER + "_0"), true)
        assertEquals(singlesNonRepeatsModel.contains(MESSAGE_PROFILE_IDENTIFIER + "_1"), true)
        assertEquals(singlesNonRepeatsModel.contains(MESSAGE_PROFILE_IDENTIFIER + "_2"), true)

        // Singles Repeats
        // --------------------------------------
        val singlesRepeatsModel = transformer.singlesRepeatsToSqlModel(mmgElementsSingleRepeats, profilesMap, modelJson)
        logger.info("singlesRepeatsModel[$MESSAGE_PROFILE_IDENTIFIER]: --> ${singlesRepeatsModel[MESSAGE_PROFILE_IDENTIFIER]}")

        assertEquals(singlesRepeatsModel.contains(MESSAGE_PROFILE_IDENTIFIER), false)
    } // .testMessageProfileIdentifier


    fun testMmgThrowsException() {

        logger.info("testMmgThrowsException..")

        assertFailsWith<InvalidConditionException>(

            block = {

                val mmgUtil = MmgUtil(redisProxy)
                val mmgs = mmgUtil.getMMGFromMessage("hl7Content", "hl7FilePath", "")
                logger.info("testMmgThrowsException: mmgs.size: ${mmgs.size}")

            } // .block

        ) // .assertFailsWith

    } // .testMmgThrowsException


    @Test
    fun testBadMessageProfIdInMMGBasedModel() {

        logger.info("testBadMessageProfIdInMMGBasedModel..")

        assertFails(

            block = {

                // MMG Based Model for the message
                // ------------------------------------------------------------------------------
                val mmgBasedModelPath = "/mmgBasedModel1BadForTest.json"
                val mmgBasedModelStr = this::class.java.getResource(mmgBasedModelPath).readText()
                val modelJson = JsonParser.parseString(mmgBasedModelStr).asJsonObject
                
                // Transformer SQL
                // ------------------------------------------------------------------------------
                val transformer = TransformerSql()

                // Message Profile Identifier
                val mesageProfIdModel = transformer.messageProfIdToSqlModel(modelJson)
                if (mesageProfIdModel.size != 3) { throw Exception("Message Profile Identifier does not have 3 parts.")}

            } // .block

        ) // .assertFails

    } // .testBadMessageProfIdInMMGBasedModel


} // .MmgSqlTest



