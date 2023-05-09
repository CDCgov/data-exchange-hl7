import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.Helper
import gov.cdc.dex.hl7.model.RedactorProcessMetadata
import gov.cdc.dex.hl7.model.RedactorReport
import gov.cdc.dex.metadata.DexMetadata
import gov.cdc.dex.metadata.Provenance
import gov.cdc.dex.util.JsonHelper.addArrayElement
import org.junit.jupiter.api.Test

class FunctionTest {

    @Test
    fun testRedactor(){
        val msg = this::class.java.getResource("/BDB_LAB_02_redact.txt")?.readText()
        val helper = Helper()
        val report = msg?.let { helper.getRedactedReport(it,"CASE") }
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
        val report =  helper.getRedactedReport(msg, "CASE")
        val w = report?._2()?.toList()

        println("w: ${w}")
        if(w != null) {
            val rw = RedactorReport(w)
//    gson.toJson(w)
//    val redactJson = w?.toJsonElement()
//
//    println("redactJson: $redactJson")

            val processMD = RedactorProcessMetadata("OK", rw, EventHubMetadata(1, 1, null, "20230101"), listOf())

            println(processMD)

            val prov = Provenance("event1", "123", "123", "test", "123", 123, "single", "a", "b", "c', 1", 1, "123")
            val md = DexMetadata(prov, listOf())

            val mdJsonStr = gson.toJson(md)

            val mdJson = JsonParser.parseString(mdJsonStr) as JsonObject
            mdJson.addArrayElement("processes", processMD)
            mdJson.addProperty("test", "test")

            println("MD: $mdJson")
        }
    }


}


