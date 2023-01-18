package gov.cdc.dex.mrr

import com.caucho.hessian.client.HessianProxyFactory
import com.google.gson.GsonBuilder
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.vocab.service.VocabService
import gov.cdc.vocab.service.bean.ValueSet
import gov.cdc.vocab.service.bean.ValueSetConcept
import redis.clients.jedis.Pipeline
import java.net.MalformedURLException
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class VocabClient(val redisProxy: RedisProxy) {
    private var service: VocabService
    private val serviceUrl = "https://phinvads.cdc.gov/vocabService/v2"
    val gson = GsonBuilder().create()

    private var phinVadsConnMS:Long = 0
    private var redisConnMS:Long = 0
    init {
        try {
            service = HessianProxyFactory().create(VocabService::class.java, serviceUrl) as VocabService
        } catch (e: MalformedURLException) {
            throw Exception("Problem in getting Hessian service:${e.printStackTrace()}")
        }
    }

    private fun getAllValueSets(): List<ValueSet> {
        var vs = listOf<ValueSet>()
        val timeInMillis = measureTimeMillis {
            val valuesResultSet = service.allValueSets
            vs =  valuesResultSet.valueSets.toList()
        }
        phinVadsConnMS += timeInMillis
        return vs
    }

    private fun getValueSetConcepts(valueSet: ValueSet): List<ValueSetConcept> {
        var vs = listOf<ValueSetConcept>()
        val timeInMillis = measureTimeMillis {
            val valueSetVersionResult = service.getValueSetVersionByValueSetOidAndVersionNumber(valueSet.oid, 0)
            val concepts =
                service.getValueSetConceptsByValueSetVersionId(valueSetVersionResult.valueSetVersion.id, 1, 950000)

            if (concepts != null && !concepts.valueSetConcepts.isNullOrEmpty())
                vs = concepts.valueSetConcepts.toList()

        }
        phinVadsConnMS += timeInMillis
        return vs
    }

//    private fun getValueSetKey(valueSet: ValueSet): StringBuilder {
//        val vocabKey = StringBuilder()
//        vocabKey.append(valueSet.code)
//        return vocabKey
//    }

    @Throws(Exception::class)
    fun setValueSetConcepts(key: String, valuesetConcepts: List<ValueSetConcept>, pipeline: Pipeline, sync:Boolean) {
        val timeInMillis = measureTimeMillis {
            try {
                print("Loading vocab:$key")
                valuesetConcepts.forEach {
                    pipeline.hset("vocab:$key", it.conceptCode, gson.toJson(it))
                }
                if (sync)
                    pipeline.sync()
                println("... Done! in $phinVadsConnMS / $redisConnMS")
            } catch (e: Exception) {
                throw Exception("Problem in setting ValuesetConcepts to Redis:${e.printStackTrace()}")
            } finally {
                //jedis.close()
            }
        }
        redisConnMS += timeInMillis
    }

    fun loadVocab() {
        phinVadsConnMS = 0
        redisConnMS = 0
        val timeInMillis = measureTimeMillis {
//            val exe = Executors.newCachedThreadPool()
            try {
                val valueSets = this.getAllValueSets()
                println("Count of ValueSets:  ${valueSets.size}")

                val pipeline = redisProxy.getJedisClient().pipelined()
                valueSets.forEachIndexed { idx, elem ->
//                    exe.submit {
                    try { //Sync every 200
                        setValueSetConcepts(elem.code, getValueSetConcepts(elem), pipeline, idx % 200 == 0 )
                    } catch (e: Exception) {
                        println("Unable to load ${elem.code}")
                    }
//                    }
                }
            } finally {
//                exe.shutdown()
            }
        }
        println("Finished loading all PHINVads in ${timeInMillis} ms, PHINVads->$phinVadsConnMS; Redis->$redisConnMS")
    }
}