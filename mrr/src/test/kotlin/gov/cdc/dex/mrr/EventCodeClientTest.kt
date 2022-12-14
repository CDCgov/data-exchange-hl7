package gov.cdc.dex.mrr

import org.junit.jupiter.api.Test



internal class EventCodeClientTest {

    @Test
    fun loadEventMapsTest() {
        val eventCodes  = EventCodeClient()
        eventCodes.loadEventMaps()
    }
    @Test
    fun loadGroupsTest() {
        val groupClient  = EventCodeClient()
        groupClient.loadGroups()
    }
}