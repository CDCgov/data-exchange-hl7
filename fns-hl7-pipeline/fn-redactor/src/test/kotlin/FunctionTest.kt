import com.google.gson.GsonBuilder
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.Helper
import gov.cdc.dex.hl7.model.RedactorReport
import gov.cdc.dex.hl7.model.RedactorStageMetadata
import org.junit.jupiter.api.Test

class FunctionTest {

    @Test
    fun testRedactor(){
        val msg = this::class.java.getResource("/BDB_LAB_02_redact.txt")?.readText()
        val helper = Helper()
        val report = msg?.let { helper.getRedactedReport(it,"NNDSS") }
        if (report != null) {
            println("report msg :${report._1}")
            println("report List: ${report._2()?.toList()}")
        }

    }

    @Test
    fun testRedactorELR(){
        val msg = this::class.java.getResource("/HL7_2.5_New HHS Fields1.txt")?.readText()
        val helper = Helper()
        val report = msg?.let { helper.getRedactedReport(it,"ELR") }
        if (report != null) {
            println("report msg :${report._1}")
            println("report List: ${report._2()?.toList()}")
        }

    }

    @Test
    fun testRedactorPHLIPVPD(){
        val msg = this::class.java.getResource("/Mumps-VPD.txt")?.readText()
        val helper = Helper()
        val report = msg?.let { helper.getRedactedReport(it,"phlip_vpd") }
        if (report != null) {
            println("report msg :${report._1}")
            println("report List: ${report._2()?.toList()}")
        }

    }
    @Test
    fun testRedactorCOVID19(){
        val msg = this::class.java.getResource("/covid25.txt")?.readText()
        val helper = Helper()
        val report = msg?.let { helper.getRedactedReport(it,"CELR") }
        if (report != null) {
            println("report msg :${report._1}")
            println("report List: ${report._2()?.toList()}")
        }

    }
    @Test
    fun extractValue(){
        val helper = Helper()
        val msg = this::class.java.getResource("/BDB_LAB_02_redact.txt").readText()
        val pidVal = helper.extractValue(msg,"PID-5[2]")
        println("pidVal: $pidVal")

    }
    @Test
    fun testMetaData(){

        val gson = GsonBuilder().serializeNulls().create()
        val msg = this::class.java.getResource("/BDB_LAB_02_redact.txt").readText()
        val helper = Helper()
        val report =  helper.getRedactedReport(msg, "NNDSS")
        val w = report?._2()?.toList()

        println("w: $w")
        if(w != null) {
            val rw = RedactorReport(w)
            val config = listOf(helper.getConfigFileName("NNDSS"))
            val stageMD = RedactorStageMetadata(rw.status, rw, EventHubMetadata(1, 1, null, "20230101"), config)

            println("Stage metadata: ${gson.toJson(stageMD)}")
        }
    }


}


