
import com.google.gson.GsonBuilder
import gov.cdc.dex.hl7.MmgValidator
import gov.cdc.dex.hl7.exception.InvalidMessageException
import gov.cdc.dex.mmg.InvalidConditionException



import org.junit.jupiter.api.Test

import java.nio.file.Files
import java.nio.file.Paths

class MMGValidatorTest {

    companion object {
        private val gsonWithNullsOn = GsonBuilder().serializeNulls().create()
    } // .companion object

    @Test
    fun testLogMMGValidatorReport() {
        val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        val mmgValidator = MmgValidator( )
        val validationReport = mmgValidator.validate(testMsg)

        println("validationReport: -->\n\n${gsonWithNullsOn.toJson(validationReport)}\n")
    } // .testLogMMGValidatorReport

    @Test
    fun testMMGValidator() {
        val filePath = "/Lyme_V1.0.2_TM_TC01.hl7"
        val testMsg = this::class.java.getResource(filePath).readText()

        val mmgValidator = MmgValidator( )
        val validationReport = mmgValidator.validate(testMsg)

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
    fun testHepMessages() {
        testFolder("Hep")
    }


    @Test
    fun testInvalidMMG() {
        try {
            val filePath = "/tbrd/BDB_LAB_13.txt"
            val testMsg = this::class.java.getResource(filePath).readText()
            //val mmgs = MmgUtil.getMMGFromMessage(testMsg, filePath, "")
            val mmgValidator = MmgValidator()
            val validationReport = mmgValidator.validate(testMsg)
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
                    val testMsg = this::class.java.getResource("/$folderName/${it.fileName.toString()}").readText()
                    //val mmgs = MmgUtil.getMMGFromMessage(testMsg, it.fileName.toString(), "")
                    val mmgValidator = MmgValidator()
                    val validationReport = mmgValidator.validate(testMsg)

                    println(validationReport)
                } catch(e: InvalidMessageException ) {
                    println(e.message)
                } catch(e:InvalidConditionException) {
                    println(e.message)
                }
                println("==========================")

            }
    }
}