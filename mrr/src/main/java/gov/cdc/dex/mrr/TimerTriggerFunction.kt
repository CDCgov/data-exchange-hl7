package gov.cdc.dex.mrr

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.TimerTrigger
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
        @TimerTrigger(name = "timerInfo", schedule = "0 0 9 * * MON") timerInfo: String?,
        context: ExecutionContext
    ) {
        context.logger.info("PhinVocabRead time trigger processed a request.")

           try {
               val client = VocabClient()
               val exe  = Executors.newCachedThreadPool()
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
                       //  context.logger.info("KEY: ${key.toString()} ")
                       // context.logger.info("KEY count: ${vsCount} ")
                       client.setValueSetConcepts(valueSetConcepts,key.toString())
                   }

                   context.logger.info("Key: ${key.toString()} + count of ${vsCount}")
              }
               context.logger.info("END OF VocabClient services")
        } catch(e:Exception){
            context.logger.info("Failure in PhinVocabRead function : ${e.printStackTrace()} ")
        }
    }

    @FunctionName("MMGATRead")
    fun runMmgat(
        @TimerTrigger(name = "timerInfo", schedule = "0 0 9 * * FRI") timerInfo: String?,
        context: ExecutionContext
    ) {
        context.logger.info("MMGATRead time trigger processed a request.")
        var jedis = Jedis()
        val parser = JsonParser()

        RedisUtility().redisConnection().use { jedis ->
            try {
                context.logger.info("Cache Response : " + jedis.ping())
                val mmgaClient = MmgatClient()
                context.logger.info("STARTING MMGATRead services")
                val mmgaGuide = mmgaClient.getGuideAll()
                val elem: JsonElement = parser.parse(mmgaGuide.toString())
                context.logger.info("Json Array size:" + elem.asJsonObject.getAsJsonArray("result").size())
                val mmgatJArray = elem.asJsonObject.getAsJsonArray("result")
                context.logger.info("Json Array size:" + mmgatJArray.size())
                val gson = GsonBuilder().create()

                for (mmgatjson in mmgatJArray) {
                    val mj = mmgatjson.asJsonObject
                    if (mj.get("guideStatus").getAsString()
                            .equals(mmgaClient.guidanceStatusUAT,true) || mj.get("guideStatus")
                            .getAsString().equals(mmgaClient.guidanceStatusFINAL,true)
                    ) {
                        val id = (mj.get("id").asString)
                        context.logger.info("MMGAT id:" + id)
                        val mGuide = mmgaClient.getGuideById(id)
                        val key = mj.get("name").getAsString()
                        if (jedis.exists(key.toString()))
                            jedis.del(key.toString())
                        jedis.set(key.toString(), gson.toJson(mGuide))

                    }
                }
            } catch (e: Exception) {
                context.logger.info("Failure in MMGATREAD function : ${e.printStackTrace()} ")
            } finally {
                jedis.close()
            }
            context.logger.info("MMGATREAD Function executed at: " + LocalDateTime.now())
        }
    }
}
