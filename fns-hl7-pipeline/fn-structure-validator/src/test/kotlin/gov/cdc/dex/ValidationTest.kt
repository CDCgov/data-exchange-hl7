package gov.cdc.dex

import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class ValidationTest {
    @Test
    fun testValidateMessage() {
        val testMessage = this::class.java.getResource("/Invalid-GenV1-0-Case-Notification.hl7").readText()
//        val phinSpec = HL7StaticParser.getFirstValue(testMessage, "MSH-21[1].1").get()
        val phinSpec =testMessage.split("\n")[0].split("|")[20].split("^")[0]
        val nistValidator = ProfileManager(ResourceFileFetcher(), "/$phinSpec")

        val report = nistValidator.validate(testMessage)
        println(report)
    }
    @Test
    fun testExtractPhinSpec() {
        val msh = "MSH|^~\\\\&|SendAppName^2.16.840.1.114222.TBD^ISO|Sending-Facility^2.16.840.1.114222.TBD^ISO|PHINCDS^2.16.840.1.114222.4.3.2.10^ISO|PHIN^2.16.840.1.114222^ISO|20140630120030.1234-0500||ORU^R01^ORU_R01|MESSAGE CONTROL ID|D|2.5.1|||||||||NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO~Lyme_TBRD_MMG_V1.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO"
        val profile = msh.split("|")[20].split("^")[0]
        println(profile)
    }


    private fun testFolder(folderName: String) {
        val dir = "src/test/resources/$folderName"

        Files.walk(Paths.get(dir))
            .filter { Files.isRegularFile(it) }
            .forEach {
                println("==================  ${it.fileName} ")
                try {
                    val testMsg = this::class.java.getResource("/$folderName/${it.fileName.toString()}").readText()

                    val phinSpec =testMsg.split("\n")[0].split("|")[20].split("^")[0]
                    val nistValidator = ProfileManager(ResourceFileFetcher(), "/$phinSpec")

                    val report = nistValidator.validate(testMsg)
                    println("Status: ${report.status}; Errors: ${report.errorCounts}; Warnings: ${report.warningcounts}")
                } catch(e: Exception) {
                    println(e.message)
                }
                println("==========================")

            }
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
}