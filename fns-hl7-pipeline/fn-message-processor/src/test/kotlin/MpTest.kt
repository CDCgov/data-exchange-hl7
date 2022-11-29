
import gov.cdc.dex.hl7.MmgUtil
import gov.cdc.hl7.HL7StaticParser

import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
import kotlin.test.assertTrue

import org.slf4j.LoggerFactory
import java.util.*

// import redis.clients.jedis.DefaultJedisClientConfig
// import redis.clients.jedis.Jedis

import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.hl7.model.ConditionCode
import gov.cdc.dex.hl7.model.PhinDataType
import gov.cdc.dex.redisModels.ValueSetConcept
// import gov.cdc.dex.hl7.model.ValueSetConcept


import gov.cdc.dex.hl7.Transformer

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.GsonBuilder

import  gov.cdc.dex.azure.RedisProxy

class MpTest {

    companion object {

        val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")
        
        val redisProxy = RedisProxy(REDIS_CACHE_NAME, REDIS_PWD)

        val redisClient = redisProxy.getJedisClient()

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

        val mmg = redisClient.get("mmg:generic_mmg_v2.0").substring(0, 100)
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
    fun testGetMMGFromMessage() {
        
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val hl7Content = this::class.java.getResource(filePath).readText()

        val mmgUtil = MmgUtil(redisProxy)
        val mmgs = mmgUtil.getMMGFromMessage(hl7Content, filePath, "messageUUID")

        logger.info("testGetMMGFromMessage: for filePath: $filePath, mmgs.size: --> ${mmgs.size}")

        mmgs.forEach {
            logger.info("testGetMMGFromMessage: MMG for filePath: $filePath, MMG name: --> ${it.name}, MMG BLOCKS: --> ${it.blocks.size}")
        }
        
        // TODO: ?
        // assertEquals(mmgs.size, 2)
    } // .testLoadMMG

    // @Test
    // fun testLoadMMGfromMessage() {

    //     val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
    //     val testMsg = this::class.java.getResource(filePath).readText()
    //     val mmgUtil = MmgUtil(redisProxy)
    //     val mmgs = mmgUtil.getMMGFromMessage(testMsg, filePath, "")

    //     mmgs.forEach {
    //         logger.info("MMG ID: ${it.id}, NAME: ${it.name}, BLOCKS: --> ${it.blocks.size}")
    //     }

    //     assertEquals(mmgs.size, 2)
    //     // assertEquals(mmgs[0].blocks.size + mmgs[1].blocks.size, 8 + 26)
    // } // .testLoadMMGfromMessage

    // TODO: ?
    // @Test
    // fun testGetRedisConditionCode() {

    //     val ccJson = redisClient.get( "condition:" + "11088" )
    //     logger.info("Redis JSON: --> $ccJson")
    //     assertTrue(ccJson.length > 0 )

    //     val cc = gson.fromJson(ccJson, ConditionCode::class.java)

    //     logger.info("Redis condition code entry: --> $cc")
    //     assertEquals(cc.mmgMaps!!.size, 1)
    // } // .testLoadMMG


    @Test
    fun testRedisMMGToClass() {

        val mmg = gson.fromJson(redisClient.get("mmg:tbrd"), MMG::class.java)
        logger.info("testRedisMMGToClass: MMG name: ${mmg.name}, blocks: ${mmg.blocks.size}")

        assertEquals(mmg.name, "TBRD")
        assertEquals(mmg.blocks.size, 26)
    } // .testLoadMMG




    @Test
    fun testTransformerHl7ToJsonModel() {

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

    } // .testTransformerHl7ToJsonModel


    @Test
    fun testPhinDataTypesToMapOfListClass() {
        val transformer = Transformer(redisProxy)

        val dataTypesMap: Map<String, List<PhinDataType>> = transformer.getPhinDataTypes()

        logger.info("testPhinDataTypesToMapOfListClass: Phin dataTypesMap.size: --> ${dataTypesMap.size}")
    } // .testPhinDataTypes
 

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


} // .MpTest
/*

    @Test
    fun testMMGUtilGetMMG() {
        val filePath = "/TBRD_V1.0.2_TM_TC01.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        val mmgs = MmgUtil.getMMGFromMessage(testMsg, filePath, "")

        logger.info("mmgs[0].name: --> ${mmgs[0].name}, mmgs[0].blocks.size: --> ${mmgs[0].blocks.size}")
        val mmgs2 = mmgs.copyOf()
        val mmgsFiltered = Transformer.getMmgsFiltered(mmgs2)
        logger.info("mmgs[0].name: --> ${mmgs[0].name}, mmgs[0].blocks.size: --> ${mmgs[0].blocks.size}")


        // when (mmgs.size) {
        //     1 -> assertEquals(mmgs[0].blocks.size, mmgsFiltered[0].blocks.size)
        //     2 -> {
        //         logger.info("mmgs[0].name: --> ${mmgs[0].name}, mmgs[0].blocks.size: --> ${mmgs[0].blocks.size}")
        //         logger.info("mmgsFiltered[0].name: --> ${mmgsFiltered[0].name}, mmgsFiltered[0].blocks.size: --> ${mmgsFiltered[0].blocks.size}")


        //         logger.info("mmgs[1].name: --> ${mmgs[1].name}, mmgs[1].blocks.size: --> ${mmgs[1].blocks.size}")
        //         logger.info("mmgsFiltered[1].name: --> ${mmgsFiltered[1].name}, mmgsFiltered[1].blocks.size: --> ${mmgsFiltered[1].blocks.size}")

        //         assertEquals(mmgs[1].blocks.size, mmgsFiltered[1].blocks.size)
        //         assertTrue( mmgs[0].blocks.size > mmgsFiltered[0].blocks.size, "fail to filter out duplicate MSH, PID from MMGs")
        //     } 
        //     3 -> {
        //         assertEquals(mmgs[2].blocks.size, mmgsFiltered[2].blocks.size)
        //         assertTrue( mmgs[1].blocks.size > mmgsFiltered[1].blocks.size, "fail to filter out duplicate MSH, PID from MMGs")
        //         assertTrue( mmgs[0].blocks.size > mmgsFiltered[0].blocks.size, "fail to filter out duplicate MSH, PID from MMGs")
        //     }
        //     else -> { 
        //         throw Exception("more than 3 MMGs found")
        //     }
        // } // .when       

        // logger.info("mmgs.size: --> ${mmgs.size}")
        // mmgs.forEach { mmg ->
        //     logger.info("mmg.name: --> ${mmg.name}, mmg.blocks.size: --> ${mmg.blocks.size}")
        //     mmg.blocks.forEach { block -> 
        //         logger.info("block.name: --> ${block.name}")
        //     }   
        // } // mmgs

        // val mmgsFiltered = Transformer.getMmgsFiltered(mmgs)
        
        // logger.info("----------------------------")

        
        // logger.info("mmgsFiltered.size: --> ${mmgsFiltered.size}")
        // mmgsFiltered.forEach { mmgFiltered ->
        //     logger.info("mmgFiltered.name: --> ${mmgFiltered.name}, mmgFiltered.blocks.size: --> ${mmgFiltered.blocks.size}")
        //     mmgFiltered.blocks.forEach { block -> 
        //         logger.info("block.name: --> ${block.name}")
        //     }   
        // } // mmgs\
    } // .testMMGUtilGetMMG



    @Test
    fun testTransformerHl7ToJsonModelwithRedisMmgTC01() {
        
        // hl7
        val hl7FilePath = "/TBRD_V1.0.2_TM_TC01.hl7"
        val hl7Content = this::class.java.getResource(hl7FilePath).readText()

        val mmgs = MmgUtil.getMMGFromMessage(hl7Content, hl7FilePath, "")

        mmgs.forEach {
            logger.info("MMG ID: ${it.id}, NAME: ${it.name}, BLOCKS: --> ${it.blocks.size}")
        }

        Transformer.hl7ToJsonModelBlocksSingle( hl7Content, mmgs )

        Transformer.hl7ToJsonModelBlocksNonSingle( hl7Content, mmgs )

    } // .testTransformerHl7ToJsonModelwithRedisMmgTC01


*/

    // @Test
    // fun testTransformerHl7ToJsonModelwithRedisMmg() {
        
    //     // hl7
    //     val hl7FilePath = "/TBRD_V1.0.2_TM_TC04.hl7"
    //     val hl7Content = this::class.java.getResource(hl7FilePath).readText()

    //     val mmgUtil = MmgUtil(redisProxy)
    //     val mmgs = mmgUtil.getMMGFromMessage(hl7Content, hl7FilePath, "")

    //     mmgs.forEach {
    //         logger.info("MMG ID: ${it.id}, NAME: ${it.name}, BLOCKS: --> ${it.blocks.size}")
    //     }

    //     val transformer = Transformer(redisProxy)
    //     val mmgModelBlocksSingle = transformer.hl7ToJsonModelBlocksSingle( hl7Content, mmgs )
    //     val mmgModelBlocksNonSingle = transformer.hl7ToJsonModelBlocksNonSingle( hl7Content, mmgs )
    //     val mmgModel = mmgModelBlocksSingle + mmgModelBlocksNonSingle 

    //     logger.info("MMG Model (mmgModel): --> ${gsonWithNullsOn.toJson(mmgModel)}\n")

    // } // .testTransformerHl7ToJsonModelwithRedisMmg

    // @Test
    // fun testRedisConcepts() {

    //     val REDIS_VOCAB_NAMESPACE = "vocab:"
    //     val key = "PHVS_YesNoUnknown_CDC" // "PHVS_ClinicalManifestations_Lyme"
    //     val conceptCode = "N"

    //     val conceptJson = jedis.hget(REDIS_VOCAB_NAMESPACE + key, conceptCode)
    //     logger.info("conceptJson: --> ${conceptJson}")

    //     val cobj:ValueSetConcept = gson.fromJson(conceptJson, ValueSetConcept::class.java)

    //     logger.info("cobj: --> ${cobj.codeSystemConceptName}, ${cobj.cdcPreferredDesignation}")

    // } // .testRedisConcepts

    // @Test
    // fun testDefaultFieldsProfile() {

    //     val phinTypes = Transformer.getPhinDataTypes()

    //     logger.info("phinTypes: --> ${phinTypes}")

    // } // .testRedisConcepts




/* 
    // @Test
    // fun testGetSegments() {
    //     val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
    //     val testMsg = this::class.java.getResource(filePath).readText()
    //     val mmgs = MmgUtil.getMMGFromMessage(testMsg, filePath, "")
    //     mmgs.forEach { mmg ->
    //         mmg.blocks.forEach { block ->
    //             block.elements.forEach { element ->
    //                 val segments = HL7StaticParser.getValue(testMsg, element.getSegmentPath())
    //                 println("--SEGMENT ${element.name}--")
    //                 if (segments.isDefined) {
    //                     segments.get().flatten().forEach { println(it) }
    //                     if (block.type in listOf("Repeat", "RepeatParentChild")) {
    //                         val allOBXs = segments.get().flatten().joinToString("\n")
    //                         val uniqueGroups = HL7StaticParser.getValue(allOBXs, "OBX-4")
    //                         if (uniqueGroups.isDefined) {
    //                             println("Unique Groups: " +uniqueGroups.get().flatten().distinct())
    //                         }
    //                     }
    //                 }
    //                 println("--END Seg ${element.name}")
    //             }
    //         }
    //     }

    // }

    // @Test
    // fun testGetLineNumber() {
    //     val testMsg = this::class.java.getResource("/Lyme_V1.0.2_TM_TC01.hl7").readText()
    //     val dataTypeSegments = HL7StaticParser.getListOfMatchingSegments(testMsg, "OBX", "@3.1='INV930'")
    //     for ( k in dataTypeSegments.keys().toList()) {
    //        println(dataTypeSegments[k].get()[5])
    //     }
    //     //Reduce to 4th group>
    //     val subList = dataTypeSegments.filter {it._2[4] == "4"}

    //     println(subList.size())
    // }

    // @Test
    // fun testInvalidCode() {

    // }
    */
