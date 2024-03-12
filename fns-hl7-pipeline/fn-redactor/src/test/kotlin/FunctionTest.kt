import com.google.gson.GsonBuilder
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.FunctionConfig
import gov.cdc.dex.hl7.Helper
import gov.cdc.dex.hl7.model.RedactorReport
import gov.cdc.dex.hl7.model.RedactorStageMetadata
import org.junit.jupiter.api.Test

class FunctionTest {
    private val fnConfig = FunctionConfig()
    private val helper = Helper()

    private fun runRedaction(dataStreamId: String, hl7FilePath: String) {
        val msg = this::class.java.getResource(hl7FilePath).readText()
        val configFile = helper.getConfigFileName(msg, fnConfig.profileConfig, dataStreamId)
        println("Config file used: $configFile")
        val report =  helper.getRedactedReport(msg ,configFile)
        if (report != null) {
            println("report msg :${report._1}")
            println("report List: ${report._2()?.toList()}")
        }
    }
    @Test
    fun testRedactor(){
        runRedaction("NNDSS", "/BDB_LAB_02_redact.txt")
    }

    @Test
    fun testRedactorELR(){
        runRedaction("CELR", "/HL7_2.5_New HHS Fields1.txt")
    }

    @Test
    fun testRedactorPHLIPVPD(){
        runRedaction("PHLIP", "/Mumps-VPD.txt")
    }
    @Test
    fun testRedactorCOVID19(){
        runRedaction("CELR", "/covid25.txt")
    }
    @Test
    fun extractValue(){
        val msg = this::class.java.getResource("/BDB_LAB_02_redact.txt").readText()
        val pidVal = helper.extractValue(msg,"PID-5[2]")
        println("pidVal: $pidVal")

    }
    @Test
    fun testMetaData(){
        val gson = GsonBuilder().serializeNulls().create()
        val msg = this::class.java.getResource("/BDB_LAB_02_redact.txt").readText()
        val configFileName = helper.getConfigFileName(msg, fnConfig.profileConfig, "NNDSS")
        val report =  helper.getRedactedReport(msg, configFileName)
        val w = report?._2()?.toList()

        println("w: $w")
        if(w != null) {
            val rw = RedactorReport(w)
            val config = listOf(configFileName)
            val stageMD = RedactorStageMetadata(rw.status, rw, EventHubMetadata(1, 1, null, "20230101"), config)

            println("Stage metadata: ${gson.toJson(stageMD)}")
        }
    }


}


