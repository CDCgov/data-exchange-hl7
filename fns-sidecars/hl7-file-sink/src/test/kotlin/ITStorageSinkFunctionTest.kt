package gov.cdc.dataexchange

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("IntegrationTest")
class ITStorageSinkFunctionTest {

    @Test
    fun testFunction() {
        var result = true
        try {
            val text = this::class.java.getResource("/test_message.json").readText()
            val messages = listOf(text)
            val function = Function()
            function.storageSink(messages)
        } catch (e : Exception) {
            result = false
        }

        assertTrue(result)

    }
}