import com.google.gson.GsonBuilder
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.DateUtil
import gov.cdc.dex.hl7.MmgValidator
import gov.cdc.dex.hl7.exception.InvalidMessageException
import gov.cdc.dex.hl7.model.MmgReport
import gov.cdc.dex.hl7.model.ReportStatus
import gov.cdc.dex.hl7.model.ValidationIssueType
import gov.cdc.dex.mmg.InvalidConditionException
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.scalactic.Bool
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue



@Tag("UnitTest")
class MMGValidatorTest {

    companion object {
        private val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
        private val REDIS_NAME = System.getenv("REDIS_CACHE_NAME")
        private val REDIS_KEY  = System.getenv("REDIS_CACHE_KEY")
        private val redisProxy = RedisProxy(REDIS_NAME, REDIS_KEY)
    } // .companion object


    @Test
    fun testInvalidMMG() {
        var exceptionThrown = false
        try {
            validateMessage("/BDB_LAB_13.txt")
        } catch (e: NoSuchElementException) {
            exceptionThrown = true
        }
        assertTrue(exceptionThrown, "Exception properly thrown - can't validate message lacking event code")
    }


    @Test
    fun testMessagesWithZeroErrorsAndWarnings() {
        val report = validateMessage("/Lyme_HappyPath.txt")
        assertEquals(ReportStatus.MMG_VALID,report.status, "Asserting report status == ${ReportStatus.MMG_VALID}")
        assertEquals(0, report.errorCount, "Asserting report error count == 0")
        assertEquals(0, report.warningCount, "Asserting report warning count == 0")
    }
    @Test
    fun testMessageWithMMGErrors() {
        val report = validateMessage("/Lyme_WithMMGErrors.txt")
        assertEquals(ReportStatus.MMG_ERRORS, report.status, "Asserting report status == ${ReportStatus.MMG_ERRORS}")
        assertTrue(report.errorCount > 0, "Asserting report error count > 0")

    }

    @Test
    fun testMessagesWithWarnings() {
        val report = validateMessage("/Lyme_WithMMGWarnings.txt")
        assertEquals( ReportStatus.MMG_VALID, report.status, "Asserting report status == ${ReportStatus.MMG_VALID}")
        assertEquals(0, report.errorCount, "Asserting report error count == 0")
        assertTrue(report.warningCount > 0, "Asserting report warning count > 0")
    }

    @Test
    fun testOBR3and4Uniqueness() {
        val report = validateMessage("/TestOBR3and4Uniqueness.txt")
        assertEquals(ReportStatus.MMG_ERRORS, report.status, "Asserting report status == ${ReportStatus.MMG_ERRORS}")
        assertEquals(1, report.errorCount, "Asserting report error count == 1")
        assertEquals(1, report.warningCount, "Asserting report warning count == 1")

        val warning = report.entries.filter { it.category == ValidationIssueType.CARDINALITY }[0]
        assertEquals(60, warning.line, "Asserting cardinality warning line number is 60")

        val error = report.entries.filter { it.category == ValidationIssueType.OBSERVATION_SUB_ID_VIOLATION }[0]
        assertEquals(60, error.line, "Asserting observation sub-id error line number is 60")
    }

    @Test
    fun testMessageWithMissingMSH212() {
        var exceptionThrown = false
        try {
            validateMessage("/Lyme_WithMissingMSH212.txt")
        } catch (e : Exception) {
            println(e.message)
            exceptionThrown = e is NoSuchElementException
        }
        assertTrue(exceptionThrown, "Asserting NoSuchElementException thrown when missing message profile ID")

    }

    @Test
    fun testMessageWithMissingJurisdiction() {
        var exceptionThrown = false
        try {
            validateMessage("/Lyme_WithMissingJurCode.txt")
        } catch (e : Exception) {
            println(e.message)
            exceptionThrown = e is NoSuchElementException
        }
        assertTrue(exceptionThrown, "Asserting NoSuchElementException thrown when missing Jurisdiction Code")
    }

    @Test
    fun testMessageWithMissingOBR31() {
        var exceptionThrown = false
        try {
           validateMessage("/Lyme_WithMissingOBR31.txt")
        } catch (e : Exception) {
            println(e.message)
            exceptionThrown = e is NoSuchElementException
        }
        assertTrue(exceptionThrown, "Asserting NoSuchElementException thrown when missing Condition Code")
    }

    @Test
    fun testMessageWithInvalidOBR31() {
        var exceptionThrown = false
        try {
            validateMessage("/Lyme_WithInvalidOBR31.txt")
        } catch (e : Exception) {
            println(e.message)
            exceptionThrown = (e is InvalidConditionException)
        }
        assertTrue(exceptionThrown, "Asserting InvalidConditionException thrown when Condition Code is invalid")
    }

    @Test
    fun testDateIsValid() {
        assertEquals("OK", DateUtil.validateHL7Date("202111"), "Asserting date 202111 is OK")
        assertEquals("OK", DateUtil.validateHL7Date("2019"), "Asserting date 2019 is OK")
        assertEquals("OK", DateUtil.validateHL7Date("20230301"), "Asserting date 20230301 is OK")
        assertFalse(DateUtil.validateHL7Date("20230229") == "OK", "Asserting date 20230229 is NOT OK")
        assertEquals("OK", DateUtil.validateHL7Date("20220812090115"), "Asserting date 20220812090115 is OK")
        assertEquals("OK", DateUtil.validateHL7Date("202302281530-0800"), "Asserting date 202302281530-0800 is OK")
        assertEquals("OK",DateUtil.validateHL7Date("202302281530+0500"), "Asserting date 202302281530+0500 is OK")
        assertEquals("OK", DateUtil.validateHL7Date("20220812090115.0"), "Asserting date 20220812090115.0 is OK")
        assertEquals("OK", DateUtil.validateHL7Date("20220812090115.02+0600"), "Asserting date 20220812090115.02+0600 is OK" )
        assertEquals("OK", DateUtil.validateHL7Date("20220812090115.1234"), "Asserting date 20220812090115.1234 is OK" )
        assertFalse(DateUtil.validateHL7Date("202208120915.02") == "OK", "Asserting date 202208120915.02 is NOT OK")
        assertFalse(DateUtil.validateHL7Date("202313300515") == "OK", "Asserting date 202313300515 is NOT OK")
    }

    @Test
    fun testMMWRWeek() {
        assertFalse(DateUtil.validateMMWRWeek("1.5") == "OK", "Asserting MMWR Week value 1.5 is NOT OK")
        assertFalse(DateUtil.validateMMWRWeek("haha") == "OK", "Asserting MMWR Week value 'haha' is NOT OK")
        assertEquals("OK", DateUtil.validateMMWRWeek("12"), "Asserting MMWR Week value 12 is OK")
        assertEquals("OK", DateUtil.validateMMWRWeek("1"), "Asserting MMWR Week value 1 is OK")
        assertEquals("OK", DateUtil.validateMMWRWeek("52"), "Asserting MMWR Week value 52 is OK")
        assertFalse(DateUtil.validateMMWRWeek("53") == "OK", "Asserting MMWR Week value 53 is NOT OK")
    }
    @Test
    fun testInvalidDate() {
        var report = validateMessage("./Lyme_BadDate.txt")
        assertEquals(report.status, ReportStatus.MMG_ERRORS, "Asserting report status == ${ReportStatus.MMG_ERRORS}")
        assertEquals(1, report.errorCount, "Asserting report error count == 1")
        assertEquals(1, report.warningCount, "Asserting report warning count == 1")
        assertEquals(report.entries.filter {  it.category == ValidationIssueType.DATE_CONTENT }.size, 2, "Asserting 2 date content errors")
    }

    @Test
    fun testInvalidWeek() {
        val report = validateMessage("./Lyme_BadWeek.txt")
        assertEquals(report.status, ReportStatus.MMG_ERRORS, "Asserting report status == ${ReportStatus.MMG_ERRORS}")
        assertEquals(1, report.errorCount, "Asserting report error count == 1")
        assertEquals(0, report.warningCount, "Asserting report warning count == 0")
        assertEquals(report.entries.filter {  it.category == ValidationIssueType.MMWR_WEEK }.size, 1, "Asserting 1 MMWR Week error")
    }

    private fun validateMessage(fileName: String): MmgReport {
        val testMsg = this::class.java.getResource(fileName).readText().trim()
        val mmgValidator = MmgValidator(redisProxy)
        val validationReport = mmgValidator.validate(testMsg)
//        println("validationReport: -->\n\n${gson.toJson(validationReport)}\n")
        return  MmgReport(validationReport)
    }
}