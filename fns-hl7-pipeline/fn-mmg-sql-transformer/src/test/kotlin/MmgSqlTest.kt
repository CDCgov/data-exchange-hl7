import gov.cdc.dex.mmg.MmgUtil
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
import gov.cdc.dex.redisModels.MMG

import gov.cdc.hl7.HL7StaticParser
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.io.path.toPath


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

        const val REDIS_INSTANCE_NAME = "ocio-ede-dev-dex-cache.redis.cache.windows.net"

        val MESSAGE_PROFILE_IDENTIFIER = "message_profile_identifier"

        const val JURISDICTION_CODE_PATH = "OBX[@3.1='77966-0']-5.1"

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
        val reportingJurisdiction = extractValue(testMsg, JURISDICTION_CODE_PATH)

        val mmgsArr = getMMGsFromMessage(testMsg, reportingJurisdiction, "10250")
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
        val reportingJurisdiction = extractValue(testMsg, JURISDICTION_CODE_PATH)


        val mmgsArr = getMMGsFromMessage(testMsg, reportingJurisdiction, "10250")

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
  /*  fun testTransformerSQLForLegacy() {

        // MMGs for the message
        // ------------------------------------------------------------------------------
        val filePath = "/Varicella_AZ_2021_1.txt"
        val testMsg = this::class.java.getResource(filePath).readText()
        val reportingJurisdiction = "13"


        val mmgsArr = getMMGsFromMessage(testMsg, reportingJurisdiction, "10030")

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
*/
    @Test
    fun testMessageProfileIdentifier() {

        // MMGs for the message
        // ------------------------------------------------------------------------------
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        val reportingJurisdiction = extractValue(testMsg, JURISDICTION_CODE_PATH)


        val mmgsArr = getMMGsFromMessage(testMsg, reportingJurisdiction, "10250")

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

                val mmgs = getMMGsFromMessage("hl7Content", "42", "00011")
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

    @Test
    fun testBadEventHubMessage() {

        logger.info("testBadEventHubMessage..")

        assertFails(

            block = {

                // MMG Based Model for the message
                // ------------------------------------------------------------------------------
                val mmgBasedModelPath = "/mmgBasedModel1BadForTest.json"
                val mmgBasedModelStr = this::class.java.getResource(mmgBasedModelPath).readText()
                

                val inputEvent = JsonParser.parseString(mmgBasedModelStr).asJsonObject
                // context.logger.info("------ inputEvent: ------>: --> $inputEvent")

                // Extract from event
                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject
                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString

                logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")

            } // .block

        ) // .assertFails

    } // .testBadEventHubMessage


    private fun extractValue(msg: String, path: String): String  {
        val value = HL7StaticParser.getFirstValue(msg, path)
        return if (value.isDefined) value.get()
        else ""
    } // .extractValue

    private fun getMMGsFromMessage(messageContent: String, jurisdictionCode: String, eventCode: String) : Array<MMG>{
        val mshProfile= extractValue(messageContent, "MSH-21[2].1")
        val mshCondition = extractValue(messageContent, "MSH-21[3].1")
        //  val eventCode = extractValue(messageContent, "OBR-31.1")
        // val jurisdictionCode = "13"
        val mmgUtil = MmgUtil(redisProxy)
        return mmgUtil.getMMGs(mshProfile, mshCondition, eventCode, jurisdictionCode)
    }
} // .MmgSqlTest



