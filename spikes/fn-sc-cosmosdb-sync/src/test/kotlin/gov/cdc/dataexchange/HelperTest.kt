package gov.cdc.dataexchange

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.HttpRequestMessage
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.*

class HelperTest {

    @Test
    fun `checkRequest returns false if headers are missing`() {
        val request: HttpRequestMessage<*>? = mock(HttpRequestMessage::class.java)
        `when`(request?.headers).thenReturn(mapOf("id" to "123"))

        assertFalse(Helper.checkRequest(request))
    }

    @Test
    fun `checkRequest returns true if headers are present`() {
        val request: HttpRequestMessage<*>? = mock(HttpRequestMessage::class.java)
        `when`(request?.headers).thenReturn(mapOf("id" to "123", "partition-key" to "key123"))

        assertTrue(Helper.checkRequest(request))
    }

    @Test
    fun `updateEventTimestamp updates the timestamp correctly`() {
        val inputJson = """
        {
            "metadata": {
                "provenance": {
                    "event_timestamp": "old_timestamp"
                }
            }
        }
        """
        val expectedNewTimestamp = "2023-10-07T10:00:00Z"

        val inputEvent: JsonObject = JsonParser.parseString(inputJson).asJsonObject
        val updatedEvent = Helper.updateEventTimestamp(inputEvent, expectedNewTimestamp)

        val updatedTimestamp = updatedEvent.getAsJsonObject("metadata")
            .getAsJsonObject("provenance")
            .get("event_timestamp")
            .asString

        assertTrue(updatedTimestamp == expectedNewTimestamp)
    }
}
