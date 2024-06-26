
import com.google.gson.GsonBuilder
import gov.cdc.dex.hl7.ValidatorFunction
import gov.cdc.hl7.HL7StaticParser
import gov.cdc.nist.validator.NistReport
import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

@Tag("UnitTest")
class ValidationTest {

    companion object {
        private val gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()
    } // .companion object

    private fun validateCaseMessage(fileName: String): NistReport {
        val testMessage = this::class.java.getResource(fileName).readText()
        val phinSpec = HL7StaticParser.getFirstValue(testMessage, "MSH-21[1].1").get().uppercase()
        val nistValidator = ProfileManager(ResourceFileFetcher(), "/profiles/NNDSS-$phinSpec")
        val report = nistValidator.validate(testMessage)
        report.status = if ("ERROR" in report.status + "") {
            "STRUCTURE_ERRORS"
        } else if (report.status.isNullOrEmpty()) {
            "Unknown"
        } else {
            report.status + ""
        }
        println("report: -->\n\n${gson.toJson(report)}\n")
        println("==============================")
        return report
    }

    @Test
    fun testPHLIPVPDMessage() {
        val testMessage = this::class.java.getResource("/VPD_Measles.txt")?.readText()
        val nistValidator = ProfileManager(ResourceFileFetcher(), "/profiles/VPD-2.5.1")
        val report = nistValidator.validate(testMessage!!)
        println("report: -->\n\n${gson.toJson(report)}\n")
        println("==============================")
    }

    @Test
    fun testValidateMessage() {
       validateCaseMessage("/FDD_V1.0.1_NTM9Crypto(F).txt")
    }

    @Test
    fun testExtractPhinSpec() {
        val msh = "MSH|^~\\&|SendAppName^2.16.840.1.114222.TBD^ISO|Sending-Facility^2.16.840.1.114222.TBD^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20140630120030.1234-0500||ORU^R01^ORU_R01|MESSAGE CONTROL ID|D|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~Lyme_TBRD_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO"
        val profile = ValidatorFunction().getProfileNameAndPaths(msh, "NNDSS").first
        println(profile)
        println("==============================")
    }

    @Test
    fun testExtractDAARTSpec() {
        val fn = ValidatorFunction()
        val msh = "MSH|^~\\&|LIMS.MN.STAGING^2.16.840.1.114222.4.3.4.23.1.2^ISO|MN Public Health Lab^2.16.840.1.114222.4.1.10080^ISO|AIMS.INTEGRATION.STG^2.16.840.1.114222.4.3.15.2^ISO|CDC.ARLN.CRECol^2.16.840.1.114222.4.1.219333^ISO|20200201104547.082-0600||ORU^R01^ORU_R01|178106199999|D|2.5.1|||||||||PHLabReport-NoAck^phLabResultsELRv251^2.16.840.1.113883.9.11^ISO"
        val profile = fn.getProfileNameAndPaths(msh, "DAART").first
        println(profile)
        println("==============================")
    }


    private fun testFolder(folderName: String) {
        testFolderByType(folderName, "NNDSS")
    }

    private fun testFolderByType(folderName: String, dataStream: String) {
        val dir = "src/test/resources/$folderName"
        val fn = ValidatorFunction()
        Files.walk(Paths.get(dir))
            .filter { Files.isRegularFile(it) }
            .forEach {
                println("==================  ${it.fileName} ")
                try {
                    val testMsg = this::class.java.getResource("/$folderName/${it.fileName}").readText()
                    val profile = fn.getProfileNameAndPaths(testMsg, dataStream)
                    val nistValidator = ProfileManager(ResourceFileFetcher(), "/profiles/${profile.first}")
                    val report = nistValidator.validate(testMsg)
                    report.status = if ("ERROR" in report.status + "") {
                        "STRUCTURE_ERRORS"
                    } else if (report.status.isNullOrEmpty()) {
                        "Unknown"
                    } else {
                        report.status + ""
                    }
                    println("Status: ${report.status}; Errors: ${report.errorCounts}; Warnings: ${report.warningcounts}")
                } catch(e: Exception) {
                    println(e.message)
                }
                println("==========================")

            }
    }

    @Test
    fun testCovidELRMessages() {
        testFolderByType("covidELR", "CELR")
    }

    @Test
    fun testTBRDMessages() {
        testFolder("tbrd")
    }


    @Test
    fun testMessageWithZeroErrorsAndWarnings() {
        val report = validateCaseMessage("/Lyme_HappyPath.txt")
        assert(report.status == "VALID_MESSAGE")
        assert(report.errorCounts?.structure   == 0)
        assert(report.errorCounts?.valueset == 0)
        assert(report.errorCounts?.content == 0)

        assert(report.warningcounts?.structure == 0)
        assert(report.warningcounts?.valueset == 0)
        assert(report.warningcounts?.content == 0)
    }

    @Test
    fun testGenV1MessageWithErrors() {
        val report = validateCaseMessage("/GenV1_withStructureErrors.txt")
        assert(report.status == "STRUCTURE_ERRORS")
        assert(report.errorCounts?.structure!! > 0)
    }

    @Test
    fun testMessageWithWarningNoErrors() {
        val report = validateCaseMessage("/Lyme_WithWarnings.txt")
        assert(report.status == "VALID_MESSAGE")
        assert(report.errorCounts?.structure   == 0)
        assert(report.errorCounts?.valueset == 0)
        assert(report.errorCounts?.content == 0)
        // max length of PID-3.1 is now 199 (incorporated from v.3.1) in v.3.0
        assert(report.warningcounts?.structure == 1)
    }

}