import gov.cdc.dex.hl7.MmgUtil
import gov.cdc.dex.hl7.MmgValidator
import org.testng.annotations.Test
import java.nio.file.Files
import java.nio.file.Paths

class MMGValidatorTest {
    @Test
    fun testMMGValidator() {
        val testMsg = this::class.java.getResource("/Lyme_V1.0.2_TM_TC01.hl7").readText()
        val mmgs = MmgUtil.getMMGFromMessage(testMsg)
        val mmgValidator = MmgValidator( testMsg, mmgs)
        val validationReport = mmgValidator.validate()

        println(validationReport);
    }

    @Test
    fun testGenV1CaseMapMessages() {
        val dir = "src/test/resources/genV1"

        Files.walk(Paths.get(dir))
            .filter { Files.isRegularFile(it) }
            .forEach {
                println("==================  ${it.fileName} ")
                val testMsg =  this::class.java.getResource("/genV1/${it.fileName.toString()}").readText()
                val mmgs = MmgUtil.getMMGFromMessage(testMsg)
                val mmgValidator = MmgValidator( testMsg, mmgs)
                val validationReport = mmgValidator.validate()

                println(validationReport)
                println("==========================")
            }
    }
}