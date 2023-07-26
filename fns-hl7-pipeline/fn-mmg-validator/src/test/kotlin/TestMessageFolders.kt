import com.google.gson.GsonBuilder
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.MmgValidator
import gov.cdc.dex.hl7.exception.InvalidMessageException
import gov.cdc.dex.hl7.model.MmgReport
import gov.cdc.dex.mmg.InvalidConditionException
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths

class TestMessageFolders {
    companion object {
        private val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
        private val REDIS_NAME = System.getenv("REDIS_CACHE_NAME")
        private val REDIS_KEY  = System.getenv("REDIS_CACHE_KEY")
        private val redisProxy = RedisProxy(REDIS_NAME, REDIS_KEY)
    } // .companion object
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
    fun testMultiOBR() {
        val report = validateMessage("/Hepatitis_V1_0_1_TM_TC01_HEP_A_Acute.txt")
        println(report)
    }
    private fun testFolder(folderName: String) {
        val dir = "src/test/resources/$folderName"
        Files.walk(Paths.get(dir))
            .filter { Files.isRegularFile(it) }
            .forEach {
                println("==================  ${it.fileName} ")
                try {
                    validateMessage("/$folderName/${it.fileName}")

                } catch(e: InvalidMessageException) {
                    println(e.message)
                } catch(e: InvalidConditionException) {
                    println(e.message)
                }
                println("==========================")

            }
    }
    private fun validateMessage(fileName: String): MmgReport {
        val testMsg = this::class.java.getResource(fileName).readText().trim()
        val mmgValidator = MmgValidator(redisProxy)
        val validationReport = mmgValidator.validate(testMsg)
//        println("validationReport: -->\n\n${gson.toJson(validationReport)}\n")
        return  MmgReport(validationReport)
    }
}