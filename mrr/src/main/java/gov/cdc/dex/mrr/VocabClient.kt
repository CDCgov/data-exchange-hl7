package gov.cdc.dex.mrr;
import com.caucho.hessian.client.HessianProxyFactory

import gov.cdc.vocab.service.VocabService
import gov.cdc.vocab.service.bean.ValueSet
import gov.cdc.vocab.service.bean.ValueSetConcept
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis
import java.net.MalformedURLException

class VocabClient {
     private var service: VocabService? = null
     private val serviceUrl = "https://phinvads.cdc.gov/vocabService/v2"

    init {
        try {
            service = HessianProxyFactory().create(VocabService::class.java, serviceUrl) as VocabService
        } catch (e: MalformedURLException) {
            throw Exception("Problem in getting Hessian service:${e.printStackTrace()}")
        }
    }

    fun getAllValueSets(): MutableList<ValueSet>? {
        val valuesResultSet =  service!!.allValueSets
        return valuesResultSet.valueSets
    }

    fun getValueSetConcepts(valueSet: ValueSet): MutableList<ValueSetConcept>? {
        val valueSetVersionResult = service!!.getValueSetVersionByValueSetOidAndVersionNumber(valueSet.oid, 0)
        val concepts = service!!.getValueSetConceptsByValueSetVersionId(valueSetVersionResult.valueSetVersion?.id, 1, 95000)
        return concepts.valueSetConcepts
    }

    fun getValueSetKey(valueSet: ValueSet): StringBuilder {
        val vocabKey = StringBuilder()
        vocabKey.append(valueSet.code)
      //  val valueSetVersionResult = service!!.getValueSetVersionByValueSetOidAndVersionNumber(valueSet.oid, 0)
       // val version = valueSetVersionResult.valueSetVersion ?.versionNumber
       // vocabKey.append("_").append("$version")
       // println("VocabKey :${vocabKey}")
        return vocabKey
    }

    @Throws(Exception::class)
    fun  setValueSetConcepts(valuesetConcepts: List<ValueSetConcept>,  key: String) {
        var jedis =Jedis()
        RedisUtility().redisConnection().use { jedis ->
            try {
                 println("Cache Response : " + jedis.ping())
                 valuesetConcepts?.let {
                        for (i in valuesetConcepts) {
                            var vkey = i.conceptCode
                            jedis.hset(key.toString(), vkey, i.toString())
                            // println("Valueset in Redis:" + key.toString() + "-" + vkey + "-" + i.toString())
                        }
                    }
            } catch (e: Exception) {
                throw Exception("Problem in setting ValuesetConcepts to Redis:${e.printStackTrace()}")
            } finally {
                jedis.close()
            }
        }
    }
}