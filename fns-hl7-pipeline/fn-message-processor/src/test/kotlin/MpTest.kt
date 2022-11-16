
import gov.cdc.dex.hl7.MmgUtil
import gov.cdc.hl7.HL7StaticParser
import org.testng.annotations.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import org.slf4j.LoggerFactory
import java.util.*

import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.hl7.model.ConditionCode
import gov.cdc.dex.hl7.Transformer

import com.google.gson.Gson

class MpTest {

    companion object {

        // val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        // val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")

        // val jedis = Jedis(REDIS_CACHE_NAME, 6380, DefaultJedisClientConfig.builder()
        // .password(REDIS_PWD)
        // .ssl(true)
        // .build())

        val logger = LoggerFactory.getLogger(MmgUtil::class.java.simpleName)
        private val gson = Gson()
    } // .companion 
/* 
    @Test
    fun testRedisInstanceUsed() {

        logger.info("REDIS_CACHE_NAME: --> ${REDIS_CACHE_NAME}")
        assertEquals(REDIS_CACHE_NAME, "tf-vocab-cache-dev.redis.cache.windows.net")
    } // .testRedisInstanceUsed


    @Test
    fun testLocalMMGToClass() {

        val mmgPath = "/Generic Version 2.0.1.json"
        val mmgJson = this::class.java.getResource(mmgPath).readText()

        val mmg = gson.fromJson(mmgJson, MMG::class.java)
        logger.info("name: ${mmg.name}, blocks: ${mmg.blocks.size}")

        assertEquals(mmg.name, "Generic Version 2.0.1")
        assertEquals(mmg.blocks.size, 8)
    } // .testLoadMMG

    @Test
    fun testRedisReadGeneric() {

        val mmg = jedis.get("mmg:generic_mmg_v2.0").substring(0, 100)
        logger.info("mmg: ${mmg}...")

        assertEquals(mmg.length, 100)
    } // .testLoadMMG


    @Test
    fun testRedisRead() {

        val mmg = jedis.get("mmg:tbrd").substring(0, 100)
        logger.info("mmg: ${mmg}...")

        assertEquals(mmg.length, 100)
    } // .testLoadMMG


    @Test
    fun testGetMMGFromMessage() {
        
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val hl7Content = this::class.java.getResource(filePath).readText()

        val mmgs = MmgUtil.getMMGFromMessage(hl7Content, filePath, "messageUUID")
        mmgs.forEach {
            logger.info("MMG Info for filePath: $filePath, MMG: --> ${it.name}, BLOCKS: --> ${it.blocks.size}")
        }

        assertEquals(mmgs.size, 2)
    } // .testLoadMMG


    @Test
    fun testGetRedisConditionCode() {

        val ccJson = jedis.get( "condition:" + "10250" )
        logger.info("Redis JSON: --> $ccJson")
        assertTrue(ccJson.length > 0 )

        val cc = gson.fromJson(ccJson, ConditionCode::class.java)

        logger.info("Redis condition code entry: --> $cc")
        assertEquals(cc.mmgMaps!!.size, 1)
    } // .testLoadMMG

    
    @Test
    fun testGetMMG() {
       // fun getMMG(msh21_2: String, msh21_3: String?, eventCode: String?, jurisdictionCode: String?): Array<MMG> {

        val mmgs = MmgUtil.getMMG("generic_mmg_v2.0", "lyme_tbrd_mmg_v1.0", "10250", "42")

        mmgs.forEach {
            logger.info("MMG Info for filePath: filePath, MMG: --> ${it.name}, BLOCKS: --> ${it.blocks.size}")
        }

        assertEquals(mmgs.size, 2)
        assertEquals(mmgs[0].blocks.size + mmgs[1].blocks.size, 8 + 34)
    } // .testLoadMMG


    @Test
    fun testRedisMMGToClass() {

        val mmg = gson.fromJson(jedis.get("mmg:tbrd"), MMG::class.java)
        logger.info("name: ${mmg.name}, blocks: ${mmg.blocks.size}")

        assertEquals(mmg.name, "TBRD")
        assertEquals(mmg.blocks.size, 34)
    } // .testLoadMMG


    @Test
    fun testLoadMMGfromMessage() {

        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        val mmgs = MmgUtil.getMMGFromMessage(testMsg, filePath, "")

        mmgs.forEach {
            logger.info("MMG ID: ${it.id}, NAME: ${it.name}, BLOCKS: --> ${it.blocks.size}")
        }

        assertEquals(mmgs.size, 2)
        assertEquals(mmgs[0].blocks.size + mmgs[1].blocks.size, 8 + 34)
    } // .testLoadMMGfromMessage

        // @Test
    // fun testMMGUtilGetMMG() {
    //     val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
    //     val testMsg = this::class.java.getResource(filePath).readText()
    //     val mmgs = MmgUtil.getMMGFromMessage(testMsg, filePath, "")
    //     mmgs.forEach { println(it)}
    // } // .testMMGUtilGetMMG

*/

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
        val hl7FilePath = "/TBRD_V1.0.2_TM_TC01.hl7"
        val hl7Content = this::class.java.getResource(hl7FilePath).readText()
        

        Transformer.hl7ToJsonModelBlocksSingle(hl7Content, mmgs)

        Transformer.hl7ToJsonModelBlocksNonSingle(hl7Content, mmgs)

    } // .testTransformerHl7ToJsonModel



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
}