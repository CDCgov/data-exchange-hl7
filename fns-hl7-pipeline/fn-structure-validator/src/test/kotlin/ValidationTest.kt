
import com.google.gson.GsonBuilder
import gov.cdc.dex.hl7.InvalidMessageException
import gov.cdc.dex.hl7.ValidatorFunction
import gov.cdc.dex.metadata.HL7MessageType
import gov.cdc.hl7.HL7StaticParser
import gov.cdc.nist.validator.NistReport
import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Paths

@Tag("UnitTest")
class ValidationTest {

    companion object {
        private val gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()
    } // .companion object

    private fun validateMessage(fileName: String): NistReport {
        val testMessage = this::class.java.getResource(fileName).readText()
        val phinSpec = HL7StaticParser.getFirstValue(testMessage, "MSH-21[1].1").get().uppercase()
     //   val phinSpec =testMessage.split("\r")[0].split("|")[20].split("^")[0]
        val nistValidator = ProfileManager(ResourceFileFetcher(), "/profiles/$phinSpec")

        println()
        val report = nistValidator.validate(testMessage)
        report.status = if ("ERROR" in report.status + "") {
            "STRUCTURE_ERRORS"
        } else if (report.status.isNullOrEmpty()) {
            "Unknown"
        } else {
            report.status + ""
        }
        println("report: -->\n\n${gson.toJson(report)}\n")
        return report
    }

    @Test
    fun testPHLIPVPDMessage() {
        val testMessage = this::class.java.getResource("/VPD_Measles.txt")?.readText()

        val nistValidator = ProfileManager(ResourceFileFetcher(), "/profiles/PHLIP_VPD-2.5.1")

        val report = nistValidator.validate(testMessage!!)
        println("report: -->\n\n${gson.toJson(report)}\n")
        //

    }


    @Test
    fun testValidateMessage() {
       validateMessage("/FDD_V1.0.1_NTM9Crypto(F).txt")
    }

    @Test
    fun testExtractPhinSpec() {
        val msh = "MSH|^~\\&|SendAppName^2.16.840.1.114222.TBD^ISO|Sending-Facility^2.16.840.1.114222.TBD^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20140630120030.1234-0500||ORU^R01^ORU_R01|MESSAGE CONTROL ID|D|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~Lyme_TBRD_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO"
        val profile = ValidatorFunction().getProfileNameAndPaths(msh, "").first
        println(profile)
    }

    @Test
    fun testExtractELRSpec() {
        val fn = ValidatorFunction()
        val msh = "MSH|^~\\&|LIMS.MN.STAGING^2.16.840.1.114222.4.3.4.23.1.2^ISO|MN Public Health Lab^2.16.840.1.114222.4.1.10080^ISO|AIMS.INTEGRATION.STG^2.16.840.1.114222.4.3.15.2^ISO|CDC.ARLN.CRECol^2.16.840.1.114222.4.1.219333^ISO|20200201104547.082-0600||ORU^R01^ORU_R01|178106199999|D|2.5.1|||||||||PHLabReport-NoAck^phLabResultsELRv251^2.16.840.1.113883.9.11^ISO"
        val profile = fn.getProfileNameAndPaths(msh, "DAART").first
        println(profile)
    }

    @Test
    fun testExtractELRDefault() {
        val fn = ValidatorFunction()
        val msh = "MSH|^~\\&#|ELIS.SC.STAG^2.16.840.1.114222.4.3.4.40.1.2^ISO|SC.Columbia.SPHL^2.16.840.1.114222.4.1.171355^ISO|US WHO Collab LabSys^2.16.840.1.114222.4.3.3.7^ISO|CDC-EPI Surv Branch^2.16.840.1.114222.4.1.10416^ISO|20221130082944.929-0500||ORU^R01^ORU_R01|OE4530T20221130082944|T|2.5.1|||NE|NE|USA||||PHLabReport-NoAck^ELR251R1_Rcvr_Prof^2.16.840.1.113883.9.11^ISO~PHLIP_ELSM_251^PHLIP_Profile_Flu^2.16.840.1.113883.9.179^ISO"
        val profile = fn.getProfileNameAndPaths(msh, "PHLIP_FLU").first
        println(profile)
    }

    @Test
    fun testExtractELRMissingMSH12() {
        val fn = ValidatorFunction()
        val msh = "MSH|^~\\&#|ELIS.SC.STAG^2.16.840.1.114222.4.3.4.40.1.2^ISO|SC.Columbia.SPHL^2.16.840.1.114222.4.1.171355^ISO|US WHO Collab LabSys^2.16.840.1.114222.4.3.3.7^ISO|CDC-EPI Surv Branch^2.16.840.1.114222.4.1.10416^ISO|20221130082944.929-0500||ORU^R01^ORU_R01|OE4530T20221130082944|T||||NE|NE|USA||||PHLabReport-NoAck^ELR251R1_Rcvr_Prof^2.16.840.1.113883.9.11^ISO~PHLIP_ELSM_251^PHLIP_Profile_Flu^2.16.840.1.113883.9.179^ISO"
        assertThrows<InvalidMessageException> {
            val profile = fn.getProfileNameAndPaths(msh, "PHLIP_FLU").first
        }

    }

    private fun testFolder(folderName: String) {
        testFolderByType(folderName, HL7MessageType.CASE)
    }

    private fun testFolderByType(folderName: String, type: HL7MessageType) {
        val dir = "src/test/resources/$folderName"

        Files.walk(Paths.get(dir))
            .filter { Files.isRegularFile(it) }
            .forEach {
                println("==================  ${it.fileName} ")
                try {
                    val testMsg = this::class.java.getResource("/$folderName/${it.fileName}").readText()
                    val phinSpec = when (type) {
                        HL7MessageType.ELR -> "COVID19_ELR-${HL7StaticParser.getFirstValue(testMsg, "MSH-12[1].1").get()}"
                        HL7MessageType.CASE -> HL7StaticParser.getFirstValue(testMsg, "MSH-21[1].1").get().uppercase()
                        else -> throw Exception("Unknown type)")
                    }
                    val nistValidator = ProfileManager(ResourceFileFetcher(), "/profiles/${phinSpec}")
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
        testFolderByType("covidELR", HL7MessageType.ELR)
    }
    fun testGenV1CaseMapMessages() {
        testFolder("genV1")
    }

    fun testArboMessages() {
        testFolder("arbo")
    }

    @Test
    fun testTBRDMessages() {
        testFolder("tbrd")
    }
    fun testNNDSSMessages() {
        testFolder("ndss")
    }

    fun testADBMessages() {
        testFolder("adb")
    }


    fun testHep10Messages() {
        testFolder("Hep")
    }

    @Test
    fun testMessageWithZeroErrorsAndWarnings() {
        val report = validateMessage("/Lyme_HappyPath.txt")
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
        val report = validateMessage("/GenV1_withStructureErrors.txt")
        assert(report.status == "STRUCTURE_ERRORS")
        assert(report.errorCounts?.structure!! > 0)
    }

    @Test
    fun testMessageWithWarningNoErrors() {
        val report = validateMessage("/Lyme_WithWarnings.txt")
        assert(report.status == "VALID_MESSAGE")
        assert(report.errorCounts?.structure   == 0)
        assert(report.errorCounts?.valueset == 0)
        assert(report.errorCounts?.content == 0)
        // max length of PID-3.1 is now 199 (incorporated from v.3.1) in v.3.0
        assert(report.warningcounts?.structure == 1)
    }

}