import com.google.gson.GsonBuilder
import gov.cdc.dex.metadata.HL7MessageType
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

    private fun validateMessage(fileName: String): NistReport {
        val testMessage = this::class.java.getResource(fileName).readText()
        val phinSpec = HL7StaticParser.getFirstValue(testMessage, "MSH-21[1].1").get().uppercase()
     //   val phinSpec =testMessage.split("\r")[0].split("|")[20].split("^")[0]
        val nistValidator = ProfileManager(ResourceFileFetcher(), "/$phinSpec")

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
    fun testELRMessage() {
        val testMessage = this::class.java.getResource("/covidELR/2.3.1 HL7 Test File with HHS Data.txt")?.readText()
        val nistValidator = ProfileManager(ResourceFileFetcher(), "/COVID19_ELR-v2.3.1")
        val report = nistValidator.validate(testMessage!!)
        println("report: -->\n\n${gson.toJson(report)}\n")
        //

    }
    @Test
    fun testPHLIPVPDMessage() {
        val testMessage = this::class.java.getResource("/VPD_Measles.txt")?.readText()

        val nistValidator = ProfileManager(ResourceFileFetcher(), "/PHLIP_VPD-v2.5.1")

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
        val msh = "MSH|^~\\\\&|SendAppName^2.16.840.1.114222.TBD^ISO|Sending-Facility^2.16.840.1.114222.TBD^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20140630120030.1234-0500||ORU^R01^ORU_R01|MESSAGE CONTROL ID|D|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~Lyme_TBRD_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO"
        //val profile = msh.split("|")[20].split("^")[0]
        val profile = HL7StaticParser.getFirstValue(msh, "MSH-21[1].1").get().uppercase()
        println(profile)
    }

    @Test
    fun testExtractELRSpec() {
        val msh = "MSH|^~\\\\&|SendAppName^2.16.840.1.114222.TBD^ISO|Sending-Facility^2.16.840.1.114222.TBD^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20140630120030.1234-0500||ORU^R01^ORU_R01|MESSAGE CONTROL ID|D|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~Lyme_TBRD_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO"
        //val profile = msh.split("|")[20].split("^")[0]
        val profile = HL7StaticParser.getFirstValue(msh, "MSH-12").get()
        println(profile)
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
                        HL7MessageType.ELR -> "COVID19_ELR-v${HL7StaticParser.getFirstValue(testMsg, "MSH-12[1].1").get()}"
                        HL7MessageType.CASE -> HL7StaticParser.getFirstValue(testMsg, "MSH-21[1].1").get().uppercase()
                        else -> throw Exception("Unknown type)")
                    }
                    val nistValidator = ProfileManager(ResourceFileFetcher(), "/${phinSpec}")
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