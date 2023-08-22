package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.DexEventPayload
import gov.cdc.dex.metadata.HL7MessageType
import org.junit.jupiter.api.Test

import java.util.logging.Logger
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
        return function.eventHubProcessor(messages, eventHubMDList, getExecutionContext())
    }

    @Test
    fun processELR_HappyPath() {
        var dexEvtPayLoad = processFile("ELR_message.txt")
        //println(dexEvtPayLoad)
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.ELR, dexEvtPayLoad.messageInfo.type)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("RECEIVED", dexEvtPayLoad.summary.currentStatus)
        assertNull(dexEvtPayLoad.summary.problem)
    }
    @Test
    fun processCASE_HappyPath() {
        var dexEvtPayLoad = processFile("CASE_message.txt")
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
        var dexEvtPayLoad = processFile("NoMetadataFile.txt")
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.UNKNOWN,dexEvtPayLoad.messageInfo.type)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("REJECTED", dexEvtPayLoad.summary.currentStatus)
        assertNotNull(dexEvtPayLoad.summary.problem)
    }
    @Test
    fun process_BatchMessage() {
        var dexEvtPayLoad = processFile("BatchMessage.txt")
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.CASE,dexEvtPayLoad.messageInfo.type)
        assertEquals("BATCH", dexEvtPayLoad.metadata.provenance.singleOrBatch)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("RECEIVED", dexEvtPayLoad.summary.currentStatus)
        assertNull(dexEvtPayLoad.summary.problem)
    }
    @Test
    fun process_InvalidMessage() {
        var dexEvtPayLoad = processFile("InvalidMessage.txt")
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.CASE,dexEvtPayLoad.messageInfo.type)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("SINGLE", dexEvtPayLoad.metadata.provenance.singleOrBatch)
        assertEquals("REJECTED", dexEvtPayLoad.summary.currentStatus)
        assertNotNull(dexEvtPayLoad.summary.problem)
    }

    @Test
    fun process_ELRWithBlanks() {
        var dexEvtPayLoad = processFile("CovidELRWithBlanks.txt")
        assertNotNull(dexEvtPayLoad)
        assertEquals(HL7MessageType.ELR, dexEvtPayLoad.messageInfo.type)
        assertNotNull(dexEvtPayLoad.metadata.processes)
        assertEquals("RECEIVED", dexEvtPayLoad.summary.currentStatus)
        assertNull(dexEvtPayLoad.summary.problem)
    }

    private fun getExecutionContext(): ExecutionContext {
        return object : ExecutionContext {
            override fun getLogger(): Logger {
                return Logger.getLogger(FunctionTest::class.java.name)
            }

            override fun getInvocationId(): String {
                return "null"
            }

            override fun getFunctionName(): String {
                return "null"
            }
        }
    }

}