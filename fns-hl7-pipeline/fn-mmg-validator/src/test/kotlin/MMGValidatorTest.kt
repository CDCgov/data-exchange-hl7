import gov.cdc.dex.hl7.MmgUtil
import gov.cdc.dex.hl7.MmgValidator
import gov.cdc.dex.hl7.exception.InvalidMessageException
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
    fun testInvalidMMG() {
        try {
            val testMsg = this::class.java.getResource("/tbrd/BDB_LAB_13.txt").readText()
            val mmgs = MmgUtil.getMMGFromMessage(testMsg)
            val mmgValidator = MmgValidator(testMsg, mmgs)
            val validationReport = mmgValidator.validate()
        } catch (e: InvalidMessageException) {
            println("Exception properly thrown - can't validate this message lacking event code")
        }
    }
    fun testFolder(folderName: String) {
        val dir = "src/test/resources/$folderName"

        Files.walk(Paths.get(dir))
            .filter { Files.isRegularFile(it) }
            .forEach {
                println("==================  ${it.fileName} ")
                try {
                    val testMsg = this::class.java.getResource("/$folderName/${it.fileName.toString()}").readText()
                    val mmgs = MmgUtil.getMMGFromMessage(testMsg)
                    val mmgValidator = MmgValidator(testMsg, mmgs)
                    val validationReport = mmgValidator.validate()

                    println(validationReport)
                } catch(e: InvalidMessageException) {
                    println(e.message)
                }
                    println("==========================")

            }
    }
}