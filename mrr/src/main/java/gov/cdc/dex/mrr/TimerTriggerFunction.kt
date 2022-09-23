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

/**
 * Azure Functions with Timer trigger.
 */
 class TimerTriggerFunction {
    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("PhinVocabRead")
    fun runVocab(
        @TimerTrigger(name = "timerInfo", schedule = "0 */10 * * * *") timerInfo: String?,
        context: ExecutionContext
    ) {
        context.logger.info("PhinVocabRead time trigger processed a request.")
        var jedis: Jedis? = null
           try {
                jedis = RedisUtility().redisConnection()

            // Simple PING command
            context.logger.info("\nCache Command  : Ping")
               if (jedis != null) {
                   context.logger.info("Cache Response : " + jedis.ping())
                   val client = VocabClient()
                   context.logger.info("STARTING VocabClient services")
                   val valueSets = client.getAllValueSets() as List<ValueSet>?

                   valueSets?.let { context.logger.info("Count of ValueSets:  ${valueSets.size}") }

                   val vocabMap: MutableMap<StringBuilder, Any> = HashMap()
                   val gson = GsonBuilder().create()
                   for (e in valueSets!!) {
                       val valueSetConcepts = client.getValueSetConcepts(e) as List<ValueSetConcept>?
                       val key = client.getValueSetKey(e)
                       vocabMap[key] = gson.toJson(valueSetConcepts)
                       if (jedis.exists(key.toString()))
                           jedis.del(key.toString())
                       jedis.set(key.toString(), gson.toJson(valueSetConcepts))
                   }
                   context.logger.info("END OF VocabClient services")

               }
               else
                context.logger.info("Failure in getting  Radis connection ")
        } catch(e:Exception){
            context.logger.log(error("Failure in PhinVocabRead function : ${e.printStackTrace()} "))
        } finally {
               jedis?.close()
        }
    }

    @FunctionName("MMGATRead")
    fun runMmgat(
        @TimerTrigger(name = "timerInfo", schedule = "0 */5 * * * *") timerInfo: String?,
        context: ExecutionContext
    ) {
        context.logger.info("MMGATRead time trigger processed a request.")
        var jedis: Jedis? = null
        //String mmgat ="";
        val parser = JsonParser()
        try {
            jedis = RedisUtility().redisConnection()
            if(jedis != null) {
                // throw RuntimeException("Radis Connection failed")
                // else {
                context.logger.info("Cache Response : " + jedis.ping())
                val mmgaClient = MmgatClient()
                context.logger.info("STARTING MMGATRead services")
                var mmgaGuide = mmgaClient.getGuideAll()
                // gson = new GsonBuilder().create();
                //  mmgat = gson.toJson(sb.toString());
                if (mmgaGuide != null) {
                    val elem: JsonElement = parser.parse(mmgaGuide.toString())
                    context.logger.info("Json Array size:" + elem.asJsonObject.getAsJsonArray("result").size())
                    val mmgatJArray = elem.asJsonObject.getAsJsonArray("result")
                    context.logger.info("Json Array size:" + mmgatJArray.size());
                    val mmgatMap = mapOf<String, String>()
                    val gson = GsonBuilder().create()

                    for (mmgatjson in mmgatJArray) {
                        val mj = mmgatjson.asJsonObject
                        if (mj.get("guideStatus").getAsString().equals("UserAcceptanceTesting") || mj.get("guideStatus")
                                .getAsString().equals("Final")
                        ) {
                            val id = (mj.get("id").getAsString())
                            context.logger.info("MMGAT id:" + id)
                            var mGuide = mmgaClient.getGuideById(id)
                            var key = mj.get("name").getAsString()
                            if (jedis.exists(key.toString()))
                                jedis.del(key.toString())
                            jedis.set(key.toString(), gson.toJson(mGuide))

                        }
                    }


                }
            }
        }catch(e:Exception){

        }finally{
            jedis?.close()
        }

        context.logger.info("MMGATREAD Function executed at: " + LocalDateTime.now());
    }
}
