
import com.google.gson.GsonBuilder
import gov.cdc.dex.hl7.MmgValidator
import gov.cdc.dex.hl7.exception.InvalidMessageException
import gov.cdc.dex.hl7.model.MmgReport
import gov.cdc.dex.hl7.model.ReportStatus
import gov.cdc.dex.mmg.InvalidConditionException



import org.junit.jupiter.api.Test

import java.nio.file.Files
import java.nio.file.Paths

class MMGValidatorTest {

    companion object {
        private val gson = GsonBuilder().serializeNulls().create()
    } // .companion object


    private fun validateMessage(fileName: String): MmgReport {
        val testMsg = this::class.java.getResource(fileName).readText()

        val mmgValidator = MmgValidator( )
        val validationReport = mmgValidator.validate(testMsg)

        println("validationReport: -->\n\n${gson.toJson(validationReport)}\n")
        return  MmgReport(validationReport)
    }
    @Test
    fun testLogMMGValidatorReport() {
        val report = validateMessage("/Lyme_V1.0.2_TM_TC01.hl7")
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
            val report = validateMessage("/tbrd/BDB_LAB_13.txt")
            assert(false)
        } catch (e: InvalidConditionException) {
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
}