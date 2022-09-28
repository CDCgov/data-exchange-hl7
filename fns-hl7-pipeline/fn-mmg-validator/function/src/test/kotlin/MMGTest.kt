
import gov.cdc.dex.hl7.MmgUtil
import org.testng.annotations.Test

class MMGTest {

    @Test
    fun testLoadMMG() {
        val mmgName = "TBRD"
        val mmgJson = MmgUtil::class.java.getResource("/" + mmgName + ".json" ).readText()
        println(mmgJson)
    }

    @Test
    fun testMMGUtilGetMMG() {
        val testMsg = this::class.java.getResource("/Lyme_V1.0.2_TM_TC01.hl7").readText()
        val mmgs = MmgUtil.getMMGFromMessage(testMsg)
        mmgs.forEach { println(it)}
    }

    @Test
    fun testInvalidCode() {

    }
}