import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.TransformerSql
import gov.cdc.dex.hl7.model.PhinDataType
import gov.cdc.dex.mmg.InvalidConditionException
import gov.cdc.dex.mmg.MmgUtil
import gov.cdc.dex.redisModels.MMG
import gov.cdc.hl7.HL7StaticParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith


class MmgSqlTest {

    companion object {
        private val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        private val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(REDIS_CACHE_NAME, REDIS_PWD)
        // val redisClient = redisProxy.getJedisClient()
        private val gson = Gson()
        private val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() 

        const val MMG_BLOCK_TYPE_SINGLE = "Single"
        const val TABLES_KEY_NAME = "tables"

        const val REDIS_INSTANCE_NAME = "ocio-ede-dev-dex-cache.redis.cache.windows.net"

        const val MESSAGE_PROFILE_IDENTIFIER = "message_profile_identifier"

        const val JURISDICTION_CODE_PATH = "OBX[@3.1='77966-0']-5.1"
        const val EVENT_CODE_PATH = "OBR-31.1"

    } // .companion 


    @Test
    fun testRedisInstanceUsed() { println("testRedisInstanceUsed: REDIS_CACHE_NAME: --> ${REDIS_CACHE_NAME}")
        assertEquals(REDIS_CACHE_NAME, REDIS_INSTANCE_NAME)
    } // .testRedisInstanceUsed


    @Test
    fun testMMGUtil() {
        println("======Begin testMMGUtil==============")
        // MMGs for the message
        // ------------------------------------------------------------------------------
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val reportingJurisdiction = extractValue(testMsg, JURISDICTION_CODE_PATH)

        val mmgsArr = getMMGsFromMessage(testMsg, reportingJurisdiction, "10250")
        println("mmgsArr.size: --> ${mmgsArr.size}")
        assertEquals(mmgsArr.size, 2)
        println("======END TEST=========================\n")
    } // .testMMGUtil


    @Test
    fun testDefaultPhinProfiles() {
        println("======Begin testDefaultPhinProfiles===========")
        // Default Phin Profiles Types
        // ------------------------------------------------------------------------------
        val dataTypesFilePath = "/DefaultFieldsProfileX.json"
        val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath).readText()
        val dataTypesMapType = object : TypeToken< Map<String, List<PhinDataType>> >() {}.type
        val profilesMap: Map<String, List<PhinDataType>> = gson.fromJson(dataTypesMapJson, dataTypesMapType)
        println("profilesMap.size: --> ${profilesMap.size}")

        assertEquals(profilesMap.size, 12)
        println("======END TEST=====================\n")
    } // .testDefaultPhinProfiles

    private fun testTransformerSQL(filePath:String, modelPath:String, printAllOutput: Boolean = false) {
        println("===Begin test of $filePath =========================================")
        val testMsg = this::class.java.getResource(filePath).readText()
        val reportingJurisdiction = extractValue(testMsg, JURISDICTION_CODE_PATH)
        val eventCode = extractValue(testMsg, EVENT_CODE_PATH)
        val mmgsArr = getMMGsFromMessage(testMsg, reportingJurisdiction, eventCode)
        // Default Phin Profiles Types
        // ------------------------------------------------------------------------------
        val dataTypesFilePath = "/DefaultFieldsProfileX.json"
        val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath).readText()
        val dataTypesMapType = object : TypeToken< Map<String, List<PhinDataType>> >() {}.type
        val profilesMap: Map<String, List<PhinDataType>> = gson.fromJson(dataTypesMapJson, dataTypesMapType)
        // MMG Based Model for the message
        // ------------------------------------------------------------------------------
        val mmgBasedModelStr = this::class.java.getResource(modelPath).readText()
        val modelJson = JsonParser.parseString(mmgBasedModelStr).asJsonObject

        if (printAllOutput) println("mmgBasedModelStr: --> $mmgBasedModelStr")

        // Transformer SQL
        // ------------------------------------------------------------------------------
        val transformer = TransformerSql()
        val mmgs = transformer.getMmgsFiltered(mmgsArr)
        val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks
        val (mmgBlocksSingle, mmgBlocksNonSingle) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }
        val ( mmgElementsSingleNonRepeats, mmgElementsSingleRepeats ) = mmgBlocksSingle.flatMap { it.elements }.partition{ !(it.isRepeat || it.mayRepeat.contains("Y"))}

        // Singles Non Repeats
        // --------------------------------------
        val singlesNonRepeatsModel = transformer.singlesNonRepeatsToSqlModel(mmgElementsSingleNonRepeats, profilesMap, modelJson)

        if (printAllOutput) println("singlesNonRepeatsModel: -->\n\n${gsonWithNullsOn.toJson(singlesNonRepeatsModel)}\n")
        println("singlesNonRepeatsModel.size: --> ${singlesNonRepeatsModel.size}")

        // Singles Repeats
        // --------------------------------------
        val singlesRepeatsModel = transformer.singlesRepeatsToSqlModel(mmgElementsSingleRepeats, profilesMap, modelJson)

        if (printAllOutput) println("singlesRepeatsModel: -->\n\n${gsonWithNullsOn.toJson(singlesRepeatsModel)}\n")
        println("singlesRepeatsModel.size: --> ${singlesRepeatsModel.size}")

        // Repeated Blocks
        // --------------------------------------
        val repeatedBlocksModel = transformer.repeatedBlocksToSqlModel(mmgBlocksNonSingle, profilesMap, modelJson)

        if (printAllOutput) println("repeatedBlocksModel: -->\n\n${gsonWithNullsOn.toJson(repeatedBlocksModel)}\n")
        println("repeatedBlocksModel.size: --> ${repeatedBlocksModel.size}")

        // Message Profile Identifier
        val messageProfIdModel = transformer.messageProfIdToSqlModel(modelJson)

        if (printAllOutput) println("messageProfIdModel: --> \n\n${gsonWithNullsOn.toJson(messageProfIdModel)}")
        println("mesageProfIdModel.size: --> ${messageProfIdModel.size}")

        val mmgSqlModel = messageProfIdModel + singlesNonRepeatsModel + mapOf(
            TABLES_KEY_NAME to singlesRepeatsModel + repeatedBlocksModel,
        ) // .mmgSqlModel
        println("mmgSqlModel.size: --> ${mmgSqlModel.size}")
        println("mmgSqlModel: -->\n${gsonWithNullsOn.toJson(mmgSqlModel)}")
        println("======END TEST============================================\n")
    }
    @Test
    fun testTransformerSQLForTC04(){
        testTransformerSQL("/TBRD_V1.0.2_TM_TC04.hl7", "/mmgBasedModel1.json", printAllOutput = false)
    }
    @Test
    fun testTransformerSQLForGenv1() {
        testTransformerSQL("/Tuleremia.hl7", "/modelGenV1.json", printAllOutput = false)
    }
    @Test
    fun testTransformerSQLForMalaria() {
        testTransformerSQL("/Malaria_V1.0.2__TC01.txt", "/modelMalaria.json", printAllOutput = false)
    }

    @Test
    fun testTransformerSQLForTB() {
        testTransformerSQL("/TB and LTBI_3-0-3_TC01.txt", "/modelTB.json")
    }

    @Test
    fun testTransformerSQLForCRS() {
        testTransformerSQL("/CRS_1-0_TC02.txt", "/modelCRS.json")
    }
    @Test
    fun testMessageProfileIdentifier() {
        println("===Begin Test Message Profile Identifier=======================")
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

        //println("mmgBasedModelStr: --> ${mmgBasedModelStr}")


        // Transformer SQL
        // ------------------------------------------------------------------------------
        val transformer = TransformerSql()

        val mmgs = transformer.getMmgsFiltered(mmgsArr)
        val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks
        val (mmgBlocksSingle, _) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }
        val ( mmgElementsSingleNonRepeats, mmgElementsSingleRepeats ) = mmgBlocksSingle.flatMap { it.elements }.partition{ !it.isRepeat }

        // Message Profile Identifier
        val messageProfIdModel = transformer.messageProfIdToSqlModel(modelJson)

        // Singles Non Repeats
        // --------------------------------------
        val singlesNonRepeatsModel = messageProfIdModel + transformer.singlesNonRepeatsToSqlModel(mmgElementsSingleNonRepeats, profilesMap, modelJson)
        println("singlesNonRepeatsModel[$MESSAGE_PROFILE_IDENTIFIER]: --> ${singlesNonRepeatsModel[MESSAGE_PROFILE_IDENTIFIER]}")
        assertEquals(singlesNonRepeatsModel.contains(MESSAGE_PROFILE_IDENTIFIER + "_0"), true)
        assertEquals(singlesNonRepeatsModel.contains(MESSAGE_PROFILE_IDENTIFIER + "_1"), true)
        assertEquals(singlesNonRepeatsModel.contains(MESSAGE_PROFILE_IDENTIFIER + "_2"), true)

        // Singles Repeats
        // --------------------------------------
        val singlesRepeatsModel = transformer.singlesRepeatsToSqlModel(mmgElementsSingleRepeats, profilesMap, modelJson)
        println("singlesRepeatsModel[$MESSAGE_PROFILE_IDENTIFIER]: --> ${singlesRepeatsModel[MESSAGE_PROFILE_IDENTIFIER]}")

        assertEquals(singlesRepeatsModel.contains(MESSAGE_PROFILE_IDENTIFIER), false)
        println("======END TEST============================\n")
    } // .testMessageProfileIdentifier

    @Test
    fun testMmgThrowsException() {
        println("testMmgThrowsException..")

        assertFailsWith<InvalidConditionException>(

            block = {

                val mmgs = getMMGsFromMessage("hl7Content", "42", "00011")
                println("testMmgThrowsException: mmgs.size: ${mmgs.size}")

            } // .block

        ) // .assertFailsWith

    } // .testMmgThrowsException


    @Test
    fun testBadMessageProfIdInMMGBasedModel() {
        println("testBadMessageProfIdInMMGBasedModel..")

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
                val messageProfIdModel = transformer.messageProfIdToSqlModel(modelJson)
                if (messageProfIdModel.size != 3) { throw Exception("Message Profile Identifier does not have 3 parts.")}

            } // .block

        ) // .assertFails

    } // .testBadMessageProfIdInMMGBasedModel

    @Test
    fun testBadEventHubMessage() {
        println("testBadEventHubMessage..")

        assertFails(

            block = {

                // MMG Based Model for the message
                // ------------------------------------------------------------------------------
                val mmgBasedModelPath = "/mmgBasedModel1BadForTest.json"
                val mmgBasedModelStr = this::class.java.getResource(mmgBasedModelPath).readText()
                

                val inputEvent = JsonParser.parseString(mmgBasedModelStr).asJsonObject
                // println("------ inputEvent: ------>: --> $inputEvent")

                // Extract from event
                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject
                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString
                println("Received and Processing messageUUID: $messageUUID, filePath: $filePath")

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



