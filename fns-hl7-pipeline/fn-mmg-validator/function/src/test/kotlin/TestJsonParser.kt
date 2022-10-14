import com.google.gson.JsonParser
import org.testng.annotations.Test

class TestJsonParser {

    @Test
    fun testParseJSON() {
        val message = this::class.java.getResource("/testEHMessages.json").readText()
        val eventArr = JsonParser.parseString(message)
        println(eventArr)
        for (i in 0..eventArr.asJsonArray.size()-1) {
            val elem = eventArr.asJsonArray[i]
            val content = elem.asJsonObject["content"]
            println(content)
            elem.asJsonObject.addProperty("newAttrib", "test123")
            val newatt = elem.asJsonObject["newAttrib"]
            println(elem)
        }

    }
}