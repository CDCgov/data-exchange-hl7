import gov.cdc.dex.hl7.MmgUtil
import gov.cdc.dex.hl7.MmgValidator
import org.testng.annotations.Test

class MMGValidatorTest {
    @Test
    fun testMMGValidator() {
        val testMsg = this::class.java.getResource("/Lyme_V1.0.2_TM_TC01.hl7").readText()
        val mmgs = MmgUtil.getMMGFromMessage(testMsg)
        val mmgValidator = MmgValidator( testMsg, mmgs)
        val validationReport = mmgValidator.validate()

        println(validationReport);

    }
}