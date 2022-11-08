
import gov.cdc.dex.hl7.MmgUtil
import gov.cdc.hl7.HL7StaticParser
import org.testng.annotations.Test
import kotlin.test.assertEquals

import org.slf4j.LoggerFactory
import java.util.*

import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

import gov.cdc.dex.hl7.model.MMG
import com.google.gson.Gson

class MMGTest {

    companion object {

        val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")

        val jedis = Jedis(REDIS_CACHE_NAME, 6380, DefaultJedisClientConfig.builder()
        .password(REDIS_PWD)
        .ssl(true)
        .build())

        val logger = LoggerFactory.getLogger(MmgUtil::class.java.simpleName)
        private val gson = Gson()
    } // .companion 

    @Test
    fun testRedisInstanceUsed() {

        logger.info("REDIS_CACHE_NAME: --> ${REDIS_CACHE_NAME}")
        assertEquals(REDIS_CACHE_NAME, "tf-vocab-cache-dev.redis.cache.windows.net")
    } // .testRedisInstanceUsed


    @Test
    fun testLoadOneMMG() {

        val mmgKeyName = "mmg:tbrd" 

        val mmgString = jedis.get( mmgKeyName )
        logger.info("mmg: ${mmgString}")

        val mmg = gson.fromJson(mmgString, MMG::class.java)
        logger.info("mmgKeyName: ${mmgKeyName}, blocks: ${mmg.blocks.size}")
    } // .testLoadMMG

    // @Test
    // fun testLoadMMGfromMessage() {

    //     val filePath = "/TBRD_V1.0.2_TM_TC01.hl7"
    //     val testMsg = this::class.java.getResource(filePath).readText()

    //     val mmgs = MmgUtil.getMMGFromMessage(testMsg, filePath, "")

    //     mmgs.forEach {
    //         logger.info("MMG ID: ${it.id}, NAME: ${it.name}, BLOCKS: --> ${it.blocks.size}")
    //     }

    // } // .testLoadMMGfromMessage

    // @Test
    // fun testMMGUtilGetMMG() {
    //     val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
    //     val testMsg = this::class.java.getResource(filePath).readText()
    //     val mmgs = MmgUtil.getMMGFromMessage(testMsg, filePath, "")
    //     mmgs.forEach { println(it)}
    // }

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