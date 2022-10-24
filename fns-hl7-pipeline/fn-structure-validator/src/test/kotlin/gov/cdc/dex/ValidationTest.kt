package gov.cdc.dex


import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.junit.jupiter.api.Test

class ValidationTest {

    @Test
    fun testValidateMessage() {
        val testMessage = this::class.java.getResource("/Invalid-GenV1-0-Case-Notification.hl7").readText()
//        val phinSpec = HL7StaticParser.getFirstValue(testMessage, "MSH-21[1].1").get()
        val phinSpec = "NND_ORU_V2.0"
        val nistValidator = ProfileManager(ResourceFileFetcher(), "/$phinSpec")

        val report = nistValidator.validate(testMessage)
        println(report)
    }
}