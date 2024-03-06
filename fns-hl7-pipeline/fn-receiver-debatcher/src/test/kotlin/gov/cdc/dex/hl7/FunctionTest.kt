package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.OutputBinding
import gov.cdc.dex.metadata.DexHL7Metadata
import org.junit.jupiter.api.Test
import java.util.logging.Logger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FunctionTest {

    private fun processFile(filename: String): DexHL7Metadata? {
        println("Start processing $filename ")
        val text = this::class.java.getResource("/$filename").readText()

        val function = Function()
        return function.processQueue(text, getExecutionContext())
    }

    @Test
    fun processELR_HappyPath() {
        val dexEvtPayLoad = processFile("ELR_message.txt")
        //println(dexEvtPayLoad)
        assertNotNull(dexEvtPayLoad)
        assertNotNull(dexEvtPayLoad.stage)
        assertEquals("RECEIVED", dexEvtPayLoad.summary.currentStatus)
        assertNull(dexEvtPayLoad.summary.problem)
    }
    @Test
    fun processCASE_HappyPath() {
        val dexEvtPayLoad = processFile("CASE_message.txt")
        //println(dexEvtPayLoad)
        assertNotNull(dexEvtPayLoad)
        assertNotNull(dexEvtPayLoad.stage)
        assertEquals("RECEIVED", dexEvtPayLoad.summary.currentStatus)
        assertNull(dexEvtPayLoad.summary.problem)
    }

    @Test
    fun process_BatchMessage() {
        val dexEvtPayLoad = processFile("BatchMessage.txt")
        assertNotNull(dexEvtPayLoad)
        assertEquals("BATCH", dexEvtPayLoad.messageMetadata.singleOrBatch)
        assertNotNull(dexEvtPayLoad.stage)
        assertEquals("RECEIVED", dexEvtPayLoad.summary.currentStatus)
        assertNull(dexEvtPayLoad.summary.problem)
    }
    @Test
    fun process_InvalidMessage() {
        val dexEvtPayLoad = processFile("InvalidMessage.txt")
        assertNotNull(dexEvtPayLoad)
        assertNotNull(dexEvtPayLoad.stage)
        assertEquals("SINGLE", dexEvtPayLoad.messageMetadata.singleOrBatch)
        assertEquals("REJECTED", dexEvtPayLoad.summary.currentStatus)
        assertNotNull(dexEvtPayLoad.summary.problem)
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
    private fun getExecutionContext():ExecutionContext {
        return object :ExecutionContext {
            override fun getLogger(): Logger {
                return Logger.getLogger(Function:: class.java.name)
            }

            override fun getInvocationId():String {
                return "null"
            }

            override fun getFunctionName():String {
                return "HL7_JSON_LAKE_TRANSFORMER"
            }
        }
    }
}