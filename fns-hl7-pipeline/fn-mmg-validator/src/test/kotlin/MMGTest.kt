
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.MmgValidator
import gov.cdc.dex.hl7.exception.InvalidConceptKey
import gov.cdc.dex.hl7.model.*
import gov.cdc.hl7.HL7StaticParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;

class MMGTest {

    @Test
    fun testLoadMMG() {
        val mmgName = "GEN_SUMMARY_CASE_MAP_v1.0"
        val mmgJson = this::class.java.getResource("/mmgs/" + mmgName + ".json" ).readText()
        println(mmgJson)
    }

    @Disabled("Remote system unavailable")
    @Test
    fun testRemoveMSH21FromGenV2() {
        val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgs = MmgValidator().getMMGFromMessage(testMsg)
        val genV2 = mmgs[0]
        val genV2NoMSH = genV2
        println(genV2NoMSH.blocks.size)
        genV2NoMSH.blocks = genV2.blocks.filter {it.name != "Message Header"}
        println(genV2NoMSH.blocks.size)
    }

    @Disabled("Remote system unavailable")
    @Test
    fun testMMGUtilGetMMG() {
        val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgs = MmgValidator().getMMGFromMessage(testMsg)
        mmgs.forEach { println(it)}
    }

    @Disabled("Remote system unavailable")
    @Test
    fun testGetSegments() {
        val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()
        val mmgs = MmgValidator().getMMGFromMessage(testMsg)
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

    @Disabled("Remote system unavailable")
    @Test
    fun testInvalidCode() {
        try {
            val REDIS_CACHE_NAME = System.getenv(RedisProxy.REDIS_CACHE_NAME_PROP_NAME)
            val REDIS_CACHE_KEY = System.getenv(RedisProxy.REDIS_PWD_PROP_NAME)
            val redisProxy = RedisProxy(REDIS_CACHE_NAME, REDIS_CACHE_KEY)
            val vocabKey = "vocab:UNKNOWN_KEY"
            val conceptStr = redisProxy.getJedisClient()
                .hgetAll(vocabKey) //?: throw InvalidConceptKey("Unable to retrieve concept values for $vocabKey")
            if (conceptStr.isNullOrEmpty()) {
                throw InvalidConceptKey("Unable to retrieve concept values for $vocabKey")
            }
            println(conceptStr)
        } catch (e: InvalidConceptKey) {
            assert(true)
            println("Exception properly thrown: ${e.message}")
        }
    }

    @Test
    fun testMMGReportError() {
        val v1 = ValidationIssue(ValidationIssueCategoryType.ERROR, ValidationIssueType.DATA_TYPE, "fieldX", "OBX[1]", 1,ValidationErrorMessage.DATA_TYPE_MISMATCH, "data type does not match" )
        val v2 = ValidationIssue(ValidationIssueCategoryType.WARNING, ValidationIssueType.DATA_TYPE, "fieldy", "OBX[2]", 2,ValidationErrorMessage.DATA_TYPE_MISMATCH, "data type does not match")
        val issues = listOf(v1, v2)

        val report = MmgReport(issues)
        println("status: ${report.status}")
        println("errors: ${report.errorCount}")
        println("warnings: ${report.warningCount}")
    }

    @Test
    fun testMMGReportWarning() {
        val v1 = ValidationIssue(ValidationIssueCategoryType.WARNING, ValidationIssueType.DATA_TYPE, "fieldX", "OBX[1]", 1,ValidationErrorMessage.DATA_TYPE_MISMATCH, "data type does not match" )
        val v2 = ValidationIssue(ValidationIssueCategoryType.WARNING, ValidationIssueType.DATA_TYPE, "fieldy", "OBX[2]", 2,ValidationErrorMessage.DATA_TYPE_MISMATCH, "data type does not match")
        val issues = listOf(v1, v2)

        val report = MmgReport(issues)
        println("status: ${report.status}")
        println("errors: ${report.errorCount}")
        println("warnings: ${report.warningCount}")
    }

    @Test
    fun testMMGReportEmpty() {
        val issues = listOf<ValidationIssue>()

        val report = MmgReport(issues)
        println("status: ${report.status}")
        println("errors: ${report.errorCount}")
        println("warnings: ${report.warningCount}")
    }
}