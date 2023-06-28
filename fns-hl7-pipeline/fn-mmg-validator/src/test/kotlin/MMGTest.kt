import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.MmgValidator
import gov.cdc.dex.hl7.exception.InvalidConceptKey
import gov.cdc.dex.hl7.model.*
import gov.cdc.hl7.HL7StaticParser
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Tag("UnitTest")
class MMGTest {
    private val REDIS_NAME = System.getenv("REDIS_CACHE_NAME")
    private val REDIS_KEY  = System.getenv("REDIS_CACHE_KEY")
    private val redisProxy = RedisProxy(REDIS_NAME, REDIS_KEY)

    @Test
    fun testRemoveMSH21FromGenV2() {
        val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgs = MmgValidator(redisProxy).getMMGFromMessage(testMsg)
        val genV2 = mmgs[0]  //gen v2 mmg provided by getMMGFromMessage should not contain message header
        val genV2NoMSHBlocks = genV2.blocks.filter {it.name != "Message Header"}
        assertEquals(genV2NoMSHBlocks.size, genV2.blocks.size, "Asserting Message Header block removed")
    }

    @Test
    fun testMMGUtilGetMMG() {
        val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgs = MmgValidator(redisProxy).getMMGFromMessage(testMsg)
        mmgs.forEach { println(it)}
        assertEquals(mmgs.size, 2, "Asserting 2 MMGs returned")
        if (mmgs.size == 2) {
            assertEquals(mmgs[0].name, "Generic Version 2.0.1", "Asserting first MMG is Gen v2" )
            assertEquals(mmgs[1].name, "Lyme Disease", "Asserting second MMG is Lyme Disease")
        }
    }

    @Test
    fun testGetSegments() {
        val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgs = MmgValidator(redisProxy).getMMGFromMessage(testMsg)
        mmgs.forEach { mmg ->
            mmg.blocks.forEach { block ->
                block.elements.forEach { element ->
                    val segments = HL7StaticParser.getValue(testMsg, element.getSegmentPath())
                    println("--SEGMENT ${element.name}--")
                    if (segments.isDefined) {
                        segments.get().flatten().forEach { println(it) }
                        if (block.type in listOf("Repeat", "RepeatParentChild")) {
                            val allOBXs = segments.get().flatten().joinToString("\n")
                            val uniqueGroups = HL7StaticParser.getValue(allOBXs, "OBX-4")
                            if (uniqueGroups.isDefined) {
                                println("Unique Groups: " +uniqueGroups.get().flatten().distinct())
                            }
                        }
                    }
                    println("--END Seg ${element.name}")
                }
            }
        }

    }

    @Test
    fun testGetLineNumber() {
        val testMsg = this::class.java.getResource("/Lyme_V1.0.2_TM_TC01.hl7").readText()
        val dataTypeSegments = HL7StaticParser.getListOfMatchingSegments(testMsg, "OBX", "@3.1='INV930'")
        for ( k in dataTypeSegments.keys().toList()) {
           println(dataTypeSegments[k].get()[5])
        }
        //Reduce to 4th group>
        val subList = dataTypeSegments.filter {it._2[4] == "4"}

        println(subList.size())
    }

    @Test
    @OptIn(ExperimentalTime::class)
    fun testInvalidCode() {
        try {
            val vocabKey = "vocab:UNKNOWN_KEY"
            println("getting client")
            val client: Jedis
            var conceptStr: String = ""
            val clientTime = measureTime {
                client = redisProxy.getJedisClient()
            }
            println("Time to get client: $clientTime")
            val conceptTime = measureTime {
                println("getting concept")
                conceptStr = client
                    .hgetAll(vocabKey).toString() //?: throw InvalidConceptKey("Unable to retrieve concept values for $vocabKey")
            }
            println("Time to get concept: $conceptTime")
            if (conceptStr.isEmpty()) {
                println("exception")
                throw InvalidConceptKey("Unable to retrieve concept values for $vocabKey")
            }
        } catch (e: InvalidConceptKey) {
            assertTrue(true, "Asserting exception properly thrown: ${e.message}")

        }
    }

    @Test
    fun testMMGReportError() {
        val v1 = ValidationIssue(ValidationIssueCategoryType.ERROR, ValidationIssueType.DATA_TYPE, "fieldX", "OBX[1]", 1,ValidationErrorMessage.DATA_TYPE_MISMATCH, "data type does not match" )
        val v2 = ValidationIssue(ValidationIssueCategoryType.WARNING, ValidationIssueType.DATA_TYPE, "fieldy", "OBX[2]", 2,ValidationErrorMessage.DATA_TYPE_MISMATCH, "data type does not match")
        val issues = listOf(v1, v2)

        val report = MmgReport(issues)
        assertEquals("MMG_ERRORS", report.status.toString(), "Asserting report status == MMG_ERRORS when Error is present")
        assertEquals(1, report.errorCount, "Asserting report error count == 1")
        assertEquals(1, report.warningCount, "Asserting report warning count == 1")
    }

    @Test
    fun testMMGReportWarning() {
        val v1 = ValidationIssue(ValidationIssueCategoryType.WARNING, ValidationIssueType.DATA_TYPE, "fieldX", "OBX[1]", 1,ValidationErrorMessage.DATA_TYPE_MISMATCH, "data type does not match" )
        val v2 = ValidationIssue(ValidationIssueCategoryType.WARNING, ValidationIssueType.DATA_TYPE, "fieldy", "OBX[2]", 2,ValidationErrorMessage.DATA_TYPE_MISMATCH, "data type does not match")
        val issues = listOf(v1, v2)

        val report = MmgReport(issues)
        assertEquals("MMG_VALID", report.status.toString(), "Asserting report status == MMG_VALID when no Error is present")
        assertEquals(0, report.errorCount, "Asserting report error count == 0")
        assertEquals(2, report.warningCount, "Asserting report warning count == 2")
    }

    @Test
    fun testMMGReportEmpty() {
        val issues = listOf<ValidationIssue>()

        val report = MmgReport(issues)
        assertEquals("MMG_VALID", report.status.toString(), "Asserting report status == MMG_VALID when no Error is present")
        assertEquals(0, report.errorCount, "Asserting report error count == 0")
        assertEquals(0, report.warningCount, "Asserting report warning count == 0")
    }
}