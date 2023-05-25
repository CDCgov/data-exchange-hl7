package gov.cdc.dex.azure

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class EventHubSenderTest {



    @Test
    fun testSendEmptyList() {
        val ehConnStr = System.getenv("EVENT_HUB_CONNECT_STR")
        val sender = EventHubSender(ehConnStr)

        sender.send("eh_unittest", listOf())
        assert(true)
    }
}