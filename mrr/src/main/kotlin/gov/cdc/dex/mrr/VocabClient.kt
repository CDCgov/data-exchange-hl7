package gov.cdc.dex.mrr

import com.caucho.hessian.client.HessianProxyFactory
import com.google.gson.GsonBuilder
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.vocab.service.VocabService
import gov.cdc.vocab.service.bean.ValueSet
import gov.cdc.vocab.service.bean.ValueSetConcept
import java.net.MalformedURLException
import java.util.concurrent.Executors

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

    private fun getAllValueSets(): MutableList<ValueSet>? {
        val valuesResultSet = service!!.allValueSets
        return valuesResultSet.valueSets
    }

    private fun getValueSetConcepts(valueSet: ValueSet): MutableList<ValueSetConcept>? {
        val valueSetVersionResult = service!!.getValueSetVersionByValueSetOidAndVersionNumber(valueSet.oid, 0)
        val concepts =
            service!!.getValueSetConceptsByValueSetVersionId(valueSetVersionResult.valueSetVersion?.id, 1, 95000)
        return concepts.valueSetConcepts
    }

    private fun getValueSetKey(valueSet: ValueSet): StringBuilder {
        val vocabKey = StringBuilder()
        vocabKey.append(valueSet.code)
        return vocabKey
    }

    @Throws(Exception::class)
    fun setValueSetConcepts(valuesetConcepts: List<ValueSetConcept>, key: String) {
        val vocabKey = "vocab:$key"
        val gson = GsonBuilder().create()
        val redisName = System.getenv("REDIS_CACHE_NAME")
        val redisKey = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(redisName, redisKey)
        // RedisUtility().redisConnection().use { jedis ->

        try {
           // println("Cache Response : " + redisProxy.getJedisClient().ping())
            valuesetConcepts.let {
                for (i in valuesetConcepts) {
                    val vkey = i.conceptCode

                    redisProxy.getJedisClient().hset(vocabKey, vkey, gson.toJson(i))
                    //jedis.del(vocabKey)
                    // println("Valueset in Redis:" + key.toString() + "-" + vkey + "-" + i.toString())
                    println("$vocabKey  is pushed to Redis")
                }
            }
        } catch (e: Exception) {
            throw Exception("Problem in setting ValuesetConcepts to Redis:${e.printStackTrace()}")
        } finally {
            //jedis.close()
        }
        //}
    }

    fun loadVocab() {
        val exe = Executors.newCachedThreadPool()
        try {
            val client = VocabClient()

            val valueSets = this.getAllValueSets() as List<ValueSet>?

            if (valueSets != null) {
                println("Count of ValueSets:  ${valueSets.size}")
            }
            var vsCount = 0
            var valueSetConcepts = listOf<ValueSetConcept>()

            for (e in valueSets!!) {
                vsCount += 1
                val key = client.getValueSetKey(e)
                if (client.getValueSetConcepts(e) != null) {
                    valueSetConcepts = (client.getValueSetConcepts(e) as List<ValueSetConcept>?)!!
                }
                exe.submit {
                    client.setValueSetConcepts(valueSetConcepts, key.toString())
                }

            }

        } catch (e: Exception) {
            throw Exception("Failure in loadvocab  :${e.printStackTrace()}")
        } finally {
            exe.shutdown()
        }
    }

}