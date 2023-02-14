package gov.cdc.dex.mrr

import com.caucho.hessian.client.HessianProxyFactory
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.vocab.service.VocabService
import org.junit.jupiter.api.Test

class PhinVadsTest {
//PHVS_WeightUnit_UCUM
    val redisName =  System.getenv("REDIS_CACHE_NAME")
    val redisKey = System.getenv("REDIS_CACHE_KEY")
    @Test
    fun testPhinVads() {
        val serviceUrl = "https://phinvads.cdc.gov/vocabService/v2"
        val service = HessianProxyFactory().create(VocabService::class.java, serviceUrl) as VocabService
        val valuesResultSet =  service.allValueSets
        val weight = valuesResultSet.valueSets.filter {  it.code == "PHVS_AdministrativeDiagnosis_CDC_ICD-10CM"}

        val valueSetVersionResult = service.getValueSetVersionByValueSetOidAndVersionNumber(weight[0].oid, 0)

        val vs = service.getValueSetConceptsByValueSetVersionId(valueSetVersionResult.valueSetVersion.id, 1 ,1000)
        println(vs)
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

    /*    @Test  //commented out so this will not run on build/package! Takes 4-evah
        fun testLoadVocab() {
            val redisProxy = RedisProxy(redisName, redisKey)
            val fn = VocabClient(redisProxy)
            fn.loadVocab()
        }

     */
}