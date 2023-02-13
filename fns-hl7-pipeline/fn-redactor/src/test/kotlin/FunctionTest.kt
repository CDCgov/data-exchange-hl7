import gov.cdc.dex.hl7.Helper
import gov.cdc.dex.hl7.model.RedactorReport
import org.junit.jupiter.api.Test

class FunctionTest {

    @Test
    fun TestRedactor(){
        val msg = this::class.java.getResource("/sample.txt").readText()
        val helper = Helper()
        val report =  helper.getRedactedReport(msg)
        if (report != null) {
            println("report msg :${report._1}")
            println("report list:${report._2}")
        }

    }
@Test
fun TestMetaData(){
    val REDACTOR_STATUS_OK = "PROCESS_REDACTOR_OK"
    val msg = this::class.java.getResource("/sample.txt").readText()
    val helper = Helper()
    val report =  helper.getRedactedReport(msg)
    val w = report?._2()?.toList()

    //println("w:${w.toArray(String)}")
    //val processMD =  RedactorReport(status =REDACTOR_STATUS_OK, report = report._2()) }
    //println("ProcessMD :$processMD")

}

}


