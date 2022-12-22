package gov.cdc.dex.mrr

import com.caucho.hessian.client.HessianProxyFactory
import gov.cdc.vocab.service.VocabService
import org.junit.jupiter.api.Test

class PhinVadsTest {

    @Test
    fun testPhinVads() {
        val serviceUrl = "https://phinvads.cdc.gov/vocabService/v2"
        val service = HessianProxyFactory().create(VocabService::class.java, serviceUrl) as VocabService
        val valuesResultSet =  service.allValueSets

        println(valuesResultSet)
    }

    @Test
    fun testPrintStackTrace() {
        try {
            throw Exception("where's the stacktrace")
        } catch (e: Exception) {
            println("Exception thrown: ${e.printStackTrace()}")
        }
    }
}