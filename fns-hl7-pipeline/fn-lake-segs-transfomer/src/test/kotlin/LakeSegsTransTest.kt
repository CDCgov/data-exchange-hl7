import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
// import kotlin.test.assertFailsWith
// import kotlin.test.assertFails

import org.slf4j.LoggerFactory
import gov.cdc.dex.hl7.TransformerSegments


class LakeSegsTransTest {

    companion object {

        val logger = LoggerFactory.getLogger(LakeSegsTransTest::class.java.simpleName)

    } // .companion 


    @Test
    fun testNothing() {
        logger.info("testing log ok")
    } // .testRedisInstanceUsed

    @Test
    fun testTransformerSegments() {
        
        val filePath = "/TBRD_V1.0.2_TM_TC04.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        val testMsg100 = testMsg.substring(0, 100)
        assertEquals(testMsg100.length, 100)

        val lakeSegsModel = TransformerSegments().hl7ToSegments(testMsg)

        // logger.info(lakeSegsModel.toString())

    } // .testTransformerSegments


} // .MmgSqlTest



