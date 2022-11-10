package gov.cdc.dex.util

import com.google.gson.JsonParser
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class JsonHelperTest {

    @Test
    fun getValueFromJson() {
        val testFile = this::class.java.getResource("/mockEventHubPayload.json").readText()
        val json = JsonParser.parseString(testFile)

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