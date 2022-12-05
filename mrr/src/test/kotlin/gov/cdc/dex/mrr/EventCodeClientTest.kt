package gov.cdc.dex.mrr

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class EventCodeClientTest {

    @Test
    fun loadEventMapsTest() {
        var event_codes : EventCodeClient = EventCodeClient()
        event_codes.loadEventMaps()
    }
}