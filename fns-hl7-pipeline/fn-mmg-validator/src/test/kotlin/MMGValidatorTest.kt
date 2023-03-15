
import com.google.gson.GsonBuilder
import gov.cdc.dex.hl7.MmgValidator
import gov.cdc.dex.hl7.exception.InvalidMessageException
import gov.cdc.dex.hl7.model.MmgReport
import gov.cdc.dex.hl7.model.ReportStatus
import gov.cdc.dex.hl7.model.ValidationIssueType
import gov.cdc.dex.mmg.InvalidConditionException



import org.junit.jupiter.api.Test

import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.NoSuchElementException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Date

class MMGValidatorTest {

    companion object {
        private val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
    } // .companion object


    private fun validateMessage(fileName: String): MmgReport {
        val testMsg = this::class.java.getResource(fileName).readText().trim()

        val mmgValidator = MmgValidator( )
        val validationReport = mmgValidator.validate(testMsg)

        println("validationReport: -->\n\n${gson.toJson(validationReport)}\n")
        return  MmgReport(validationReport)
    }
    @Test
    fun testLogMMGValidatorReport() {
        val report = validateMessage("/arbo/ARBOVIRAL_V1_3_TM_CN_TC01.txt")
    } // .testLogMMGValidatorReport


    @Test
    fun testGenV1CaseMapMessages() {
        testFolder("genV1")
    }

    @Test
    fun testArboMessages() {
        testFolder("arbo")
    }

    @Test
    fun testTBRDMessages() {
        testFolder("tbrd")
    }
    @Test
    fun testNNDSSMessages() {
        testFolder("ndss")
    }
    @Test
    fun testADBMessages() {
        testFolder("adb")
    }

    @Test
    fun testHepMessages() {
        testFolder("Hep")
    }


    @Test
    fun testInvalidMMG() {
        try {
            val report = validateMessage("/BDB_LAB_13.txt")
            assert(false)
        } catch (e: NoSuchElementException) {
            println("Exception properly thrown - can't validate this message lacking event code")
            assert(true)
        }
    }
    private fun testFolder(folderName: String) {
        val dir = "src/test/resources/$folderName"

        Files.walk(Paths.get(dir))
            .filter { Files.isRegularFile(it) }
            .forEach {
                println("==================  ${it.fileName} ")
                try {
                    val report = validateMessage("/$folderName/${it.fileName}")

                } catch(e: InvalidMessageException ) {
                    println(e.message)
                } catch(e:InvalidConditionException) {
                    println(e.message)
                }
                println("==========================")

            }
    }

    @Test
    fun testMessagesWithZeroErrorsAndWarnings() {
        val report = validateMessage("/Lyme_HappyPath.txt")
        assert(report.status == ReportStatus.MMG_VALID)
        assert(report.errorCount == 0)
        assert(report.warningCount == 0)
    }
    @Test
    fun testMessageWithMMGErrors() {
        val report = validateMessage("/Lyme_WithMMGErrors.txt")
        assert(report.status == ReportStatus.MMG_ERRORS)
        assert(report.errorCount > 0)

    }

    @Test
    fun testMessagesWithWarnings() {
        val report = validateMessage("/Lyme_WithMMGWarnings.txt")
        assert(report.status == ReportStatus.MMG_VALID)
        assert(report.errorCount == 0)
        assert(report.warningCount > 0)
    }

    @Test
    fun testUTF() {
        println(MmgValidator.REPORTING_JURISDICTION_PATH)
    }

    @Test
    fun testOBR3and4Uniqueness() {
        val report = validateMessage("/TestOBR3and4Uniqueness.txt")
        assert(report.status == ReportStatus.MMG_ERRORS)
        assert(report.errorCount == 1)
        assert(report.warningCount == 1)

        val warning = report.entries.filter { it.category == ValidationIssueType.CARDINALITY }[0]
        assert(warning.line == 60)

        val error = report.entries.filter { it.category == ValidationIssueType.OBSERVATION_SUB_ID_VIOLATION }[0]
        assert(error.line == 60)
    }

    @Test
    fun testMessageWithMissingMSH212() {
        try {
            val report = validateMessage("/Lyme_WithMissingMSH212.txt")
        } catch (e : Exception) {
            println(e.message)
            assert(e is NoSuchElementException)
        }
    }

    @Test
    fun testMessageWithMissingJursidiction() {
        try {
            val report = validateMessage("/Lyme_WithMissingJurCode.txt")
        } catch (e : Exception) {
            println(e.message)
            assert(e is NoSuchElementException)
        }
    }

    @Test
    fun testMessageWithMissingOBR31() {
        try {
            val report = validateMessage("/Lyme_WithMissingOBR31.txt")
        } catch (e : Exception) {
            println(e.message)
            assert(e is NoSuchElementException)
        }
    }

    @Test
    fun testMessageWithInvalidOBR31() {
        try {
            val report = validateMessage("/Lyme_WithInvalidOBR31.txt")
        } catch (e : Exception) {
            println(e.message)
            assert(e is InvalidConditionException)
        }
    }

    @Test
    fun testDateIsValid() {
        var exceptionThrown = false
        val dateString = "20230229"
        /*
            valid patterns:
            for DT
               uuuu
               uuuuMM
               uuuuMMdd

            for DTM or TS
               uuuu
               uuuuMM
               uuuuMMdd
               uuuuMMddHHmm
               uuuuMMddHHmmss
               uuuuMMddHHmmss.S
               uuuuMMddHHmmss.SS
               uuuuMMddHHmmss.SSS
               uuuuMMddHHmmss.SSSS

           any of the above may include +/-Z
         */
        val formatter = DateTimeFormatter.ofPattern("uuuuMMdd[Z]").withResolverStyle(ResolverStyle.STRICT)
        try {
            val localDate = LocalDate.parse(dateString, formatter)
            println(localDate)
        } catch (e: Exception) {
            exceptionThrown = true
            println(e.message)
        }
        assert(exceptionThrown)

        exceptionThrown = false
        val dateString2 = "20230228+0800"
        try {
            val localDate2 = LocalDate.parse(dateString2, formatter)
        } catch (e: Exception) {
            exceptionThrown = true
        }
        assert(!exceptionThrown)

        exceptionThrown = false
        val dateTimeString = "202302280115"
        val dateTimeString2 = "202302281530+0800"
        val dtf = DateTimeFormatter.ofPattern("uuuuMMddHHmm[Z]").withResolverStyle(ResolverStyle.STRICT)
        try {
            val localDt = LocalDateTime.parse(dateTimeString, dtf)
            val localDt2 = LocalDateTime.parse(dateTimeString2, dtf)
            println(localDt)
            println(localDt2)
        } catch( e: Exception ) {
            exceptionThrown = true
            println(e.message)
        }
        assert(!exceptionThrown)
    }
}