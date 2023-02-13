import com.google.gson.*
import com.google.gson.reflect.TypeToken
import gov.cdc.dex.hl7.Helper
import gov.cdc.dex.hl7.model.RedactorProcessMetadata
import gov.cdc.dex.metadata.DexMetadata
import gov.cdc.dex.metadata.ProcessMetadata
import gov.cdc.dex.metadata.Provenance
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.RedactInfo
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

    val gson = GsonBuilder().serializeNulls().create()

    val REDACTOR_STATUS_OK = "PROCESS_REDACTOR_OK"
    val msg = this::class.java.getResource("/sample.txt").readText()
    val helper = Helper()
    val report =  helper.getRedactedReport(msg)
    val w = report?._2()?.toList()

    println("w: ${w}")
//    gson.toJson(w)
//    val redactJson = w?.toJsonElement()
//
//    println("redactJson: $redactJson")

    val processMD = RedactorProcessMetadata("OK", w)

    println(processMD)

    val prov = Provenance("event1", "123", "123", "test", "123", 123, "single", "a", "b", "c', 1")
    val md = DexMetadata(prov, listOf())

    val mdJsonStr = gson.toJson(md)

    val mdJson = JsonParser.parseString(mdJsonStr) as JsonObject
    mdJson.addArrayElement("processes" , processMD)

    println("MD: $mdJson")

}
//
//    fun Any.toJsonElement2(): JsonElement {
//        val jsonStr = GsonBuilder().serializeNulls().create().toJson(this)
//        return JsonParser.parseString(jsonStr)
//    }
//
//    fun JsonObject.addArrayElement2(arrayName: String, processMD: ProcessMetadata) {
//        val currentProcessPayload = this[arrayName]
//        if (currentProcessPayload == null) {
//            this.add(arrayName,  JsonArray())
//        }
//        val currentArray = this[arrayName].asJsonArray
//        currentArray.add(processMD.toJsonElement2())
//    }

}


