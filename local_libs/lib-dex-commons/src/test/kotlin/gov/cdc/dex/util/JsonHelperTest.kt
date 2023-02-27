package gov.cdc.dex.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gov.cdc.dex.metadata.DexMessageInfo
import gov.cdc.dex.metadata.HL7MessageType
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import kotlin.reflect.typeOf

internal class JsonHelperTest {

    @Test
    fun getArrayOfStringFromJsonArray() {
        val dmi = DexMessageInfo("10030", "some route", listOf("varicella_message_mapping_guide_v2_01", "VaricellaCaseNationalNotificationMap_v1_0"),
          "13", HL7MessageType.CASE)
        val gson = Gson()
        val dmiString = gson.toJson(dmi)
        val jsonObj = JsonParser.parseString(dmiString) as JsonObject
        println(jsonObj["mmgs"])
        println(jsonObj["mmgs"].javaClass.name)
        val mmgs = JsonHelper.getStringArrayFromJsonArray(jsonObj["mmgs"].asJsonArray)
        assert(mmgs is Array<String>)
    }
    @Test
    fun getValueFromJson() {
        val testFile = this::class.java.getResource("/mockEventHubPayload.json").readText()
        val json = JsonParser.parseString(testFile) as JsonObject

        val nullAttr = json["null_attr"]
        println(nullAttr.asJsonNull)

        val test = JsonHelper.getValueFromJson("data.url", json)
        assert(test.asString == "https://my-st-acct...new-file.txt")
        println(test.asString)
        println(test.javaClass.name)

        val data = JsonHelper.getValueFromJson("data", json)
        assert(data.toString().startsWith("{\"api\":\"PutBlockList\",\"clientReques"))
        println(data)
        println(data.javaClass.name)

        val root = JsonHelper.getValueFromJson("eventType", json)
        assert(root.asString == "Microsoft.Storage.BlobCreated")
        println(root)
        println(root.javaClass.name)

        val threeDeep = JsonHelper.getValueFromJson("data.storageDiagnostics.batchId", json)
        assert(threeDeep.asString == "b68529f3-68cd-4744-...")
        println(threeDeep.asString)
        println(threeDeep.javaClass.name)

        try {
            JsonHelper.getValueFromJson("data.b", json)
        } catch (e: UnknownPropertyError) {
            assertTrue(true, "Exception properly thrown: ${e.message}")
            println("Exception properly thrown: ${e.message}")
        }
    }
}