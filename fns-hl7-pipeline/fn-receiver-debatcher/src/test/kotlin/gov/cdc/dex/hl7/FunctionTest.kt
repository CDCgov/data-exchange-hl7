package gov.cdc.dex.hl7

import com.google.gson.JsonObject
import com.microsoft.azure.functions.OutputBinding
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.DexEventPayload
import gov.cdc.dex.metadata.HL7MessageType
import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FunctionTest {

    private fun processFile(filename: String): DexEventPayload? {
        println("Start processing $filename ")
        val text = this::class.java.getResource("/$filename").readText()
        val messages = listOf(text)
        val eventHubMDList = listOf(EventHubMetadata(1, 99, "", ""))

        val function = Function()
        return function.eventHubProcessor(messages, eventHubMDList,
            getOutputBinding<List<String>>(),
            getOutputBinding<List<String>>(),
            getOutputBinding<List<JsonObject>>())
    }

    @Test
    fun processELR_HappyPath() {
        val dexEvtPayLoad = processFile("ELR_message.txt")
        //println(dexEvtPayLoad)
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.ELR, dexEvtPayLoad.messageInfo.type)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("RECEIVED", dexEvtPayLoad.summary.currentStatus)
        assertNull(dexEvtPayLoad.summary.problem)
    }
    @Test
    fun processCASE_HappyPath() {
        val dexEvtPayLoad = processFile("CASE_message.txt")
        //println(dexEvtPayLoad)
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.CASE,dexEvtPayLoad.messageInfo.type)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("RECEIVED", dexEvtPayLoad.summary.currentStatus)
        assertNull(dexEvtPayLoad.summary.problem)
    }
//    @Test
//    fun process_ErrorPath() {
//        var dexEvtPayLoad = processFile("ERROR_message.txt")
//        println(dexEvtPayLoad)
//    }
    @Test
    fun process_NoMetadata() {
        val dexEvtPayLoad = processFile("NoMetadataFile.txt")
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.UNKNOWN,dexEvtPayLoad.messageInfo.type)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("REJECTED", dexEvtPayLoad.summary.currentStatus)
        assertNotNull(dexEvtPayLoad.summary.problem)
    }
    @Test
    fun process_BatchMessage() {
        val dexEvtPayLoad = processFile("BatchMessage.txt")
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.CASE,dexEvtPayLoad.messageInfo.type)
        assertEquals("BATCH", dexEvtPayLoad.metadata.provenance.singleOrBatch)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("RECEIVED", dexEvtPayLoad.summary.currentStatus)
        assertNull(dexEvtPayLoad.summary.problem)
    }
    @Test
    fun process_InvalidMessage() {
        val dexEvtPayLoad = processFile("InvalidMessage.txt")
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.CASE,dexEvtPayLoad.messageInfo.type)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("SINGLE", dexEvtPayLoad.metadata.provenance.singleOrBatch)
        assertEquals("REJECTED", dexEvtPayLoad.summary.currentStatus)
        assertNotNull(dexEvtPayLoad.summary.problem)
    }

    @Test
    fun process_ELRWithBlanks() {
        val dexEvtPayLoad = processFile("CovidELRWithBlanks.txt")
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.ELR, dexEvtPayLoad.messageInfo.type)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("RECEIVED", dexEvtPayLoad.summary.currentStatus)
        assertNull(dexEvtPayLoad.summary.problem)
    }

    private fun <T> getOutputBinding(): OutputBinding<T> {
        return object : OutputBinding<T> {
            var inner : T? = null
            override fun getValue(): T ? {
                return inner
            }

            override fun setValue(p0:T?) {
                inner = p0
            }
        }
    }
}