package gov.cdc.dex.mrr

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.TimerTrigger
import gov.cdc.dex.util.StringUtils
import gov.cdc.vocab.service.bean.ValueSet
import gov.cdc.vocab.service.bean.ValueSetConcept
import redis.clients.jedis.Jedis
import java.time.LocalDateTime
import java.util.concurrent.Executors

/**
 * Azure Functions with Timer trigger.
 */
class TimerTriggerFunction {
    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("PhinVocabRead")
    fun runVocab(
        @TimerTrigger(name = "timerInfo", schedule = "%PHINVOCAB_TIME_TRIGGER%") timerInfo: String?,
        context: ExecutionContext
    ) {
        context.logger.info("PhinVocabRead time trigger processed a request.")
        val exe = Executors.newCachedThreadPool()
        //checking Redis Connection
        try {
            var jedis = RedisUtility().redisConnection()
        } catch (e: Exception) {
            context.logger.info("Redis Connection failure:${e.printStackTrace()}")
            throw Exception("PhinVocab Function failure")
        }

        try {
            val client = VocabClient()
            context.logger.info("STARTING VocabClient services")
            val valueSets = client.getAllValueSets() as List<ValueSet>?

            if (valueSets != null) {
                context.logger.info("Count of ValueSets:  ${valueSets.size}")
            }
            var vsCount =0
            var valueSetConcepts = listOf<ValueSetConcept>()

            for (e in valueSets!!) {
                vsCount += 1
                val key = client.getValueSetKey(e)
                if(client.getValueSetConcepts(e) != null) {
                    valueSetConcepts = (client.getValueSetConcepts(e) as List<ValueSetConcept>?)!!
                }
                exe.submit {
                    client.setValueSetConcepts(valueSetConcepts,key.toString())
                }

                context.logger.info("Key: $key + count of $vsCount")
            }
            context.logger.info("END OF VocabClient services")
        } catch(e:Exception){
            context.logger.info("Failure in PhinVocabRead function : ${e.printStackTrace()} ")
            throw Exception("Failure in PhinVocabRead function ::${e.printStackTrace()}")
        }
        finally{
            exe.shutdown()
        }

    }

    @FunctionName("MMGATRead")
    fun runMmgat(
        @TimerTrigger(name = "timerInfo", schedule = "%MMGAT_TIME_TRIGGER%") timerInfo: String?,
        context: ExecutionContext
    ) {
        context.logger.info("MMGATRead time trigger processed a request.")
        var jedis = Jedis()
        val parser = JsonParser()
        //load legacy MMGAT'S
        MmgatClient().loadLegacyMmgat()
        RedisUtility().redisConnection().use { jedis ->
            try {
                context.logger.info("Cache Response : " + jedis.ping())
                val mmgaClient = MmgatClient()
                context.logger.info("STARTING MMGATRead services")

                val mmgaGuide = mmgaClient.getGuideAll().toString()

                val elem: JsonElement = parser.parse(mmgaGuide.toString())
                //context.logger.info("Json Array size:" + elem.asJsonObject.getAsJsonArray("result").size())
                val mmgatJArray = elem.asJsonObject.getAsJsonArray("result")
                context.logger.info("Json Array size:" + mmgatJArray.size())
                val gson = GsonBuilder().create()

                for (mmgatjson in mmgatJArray) {
                    val mj = mmgatjson.asJsonObject
                    if (mj.get("guideStatus").asString
                            .equals(mmgaClient.GUIDANCE_STATUS_UAT,true) || mj.get("guideStatus")
                            .asString.equals(mmgaClient.GUIDANCE_STATUS_FINAL,true)
                    ) {
                        val id = (mj.get("id").asString)
                        // context.logger.info("MMGAT id:$id")
                        val mGuide = mmgaClient.getGuideById(id)
                        val melement = parser.parse(mGuide.toString())
                        val mresult = melement.asJsonObject.get("result")
//                          var kSet =  mresult.asJsonObject.keySet()
//                        var i :Iterator<String>  = kSet.iterator()
//                         while(i.hasNext()){
//                             var kName = i.next().toString()
//                             context.logger.info("Kname---- -- $kName")
//
//                         }
                        mresult.asJsonObject.remove("testScenarios")
                        mresult.asJsonObject.remove("testCaseScenarioWorksheetColumns")
                        mresult.asJsonObject.remove("columns")
                        mresult.asJsonObject.remove("templates")
                        mresult.asJsonObject.remove("valueSets")

                        //var mresult1 = mresult.toString().substring(0, 1000)
                        //context.logger.info("MMGAT result---- -- $mresult")

                        //val version1 = mj.get("publishVersion").asString
                        val key = "mmg:"+ StringUtils.normalizeString(mj.get("name").asString)
                        context.logger.info("MMGAT name1:$key")
                        if (jedis.exists(key))
                            jedis.del(key)
                        jedis.set(key, gson.toJson(mresult))

                    }

                }

            } catch (e: Exception) {
                context.logger.info("Failure in MMGATREAD function : ${e.printStackTrace()} ")
                throw e
            } finally {
                jedis.close()
            }
            context.logger.info("MMGATREAD Function executed at: " + LocalDateTime.now())

        }
    }
}