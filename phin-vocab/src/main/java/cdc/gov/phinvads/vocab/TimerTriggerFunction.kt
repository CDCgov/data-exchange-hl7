package cdc.gov.phinvads.vocab

import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.TimerTrigger
import gov.cdc.vocab.service.bean.ValueSet
import gov.cdc.vocab.service.bean.ValueSetConcept
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis
import java.util.*


/**
 * Azure Functions with Timer trigger.
 */
class TimerTriggerFunction {
    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("VocabReadFunction")
    fun run(
        @TimerTrigger(name = "timerInfo", schedule = "0 30 16 * * FRI") timerInfo: String?,
        context: ExecutionContext
   ) {
               context.logger.info("Java time trigger processed a request.")
               val redisCacheName = System.getenv("REDISCACHEHOSTNAME")
               val rediscachekey = System.getenv("REDISCACHEKEY")
               // redisCacheName = "temedehl7.redis.cache.windows.net"
               // rediscachekey ="1ofYheE07YlzGOuDAauSQwQ09tj5u4fVrAzCaKAsQt0="
                context.logger.info("cacheHostname :${redisCacheName} ")

               // Connect to the Azure Cache for Redis over the TLS/SSL port using the key.
               val jedis = Jedis(
                   redisCacheName, 6380, DefaultJedisClientConfig.builder()
                       .password(rediscachekey)
                       .ssl(true)
                       .build()
               )
               // Simple PING command
               context.logger.info( "\nCache Command  : Ping" )
               context.logger.info( "Cache Response : " + jedis.ping())
               //println( "Cache Response : " + jedis.set("Message", "Test"))

               val client = VocabClient()
               context.logger.info("STARTING VocabClient services")
               val valueSets = client.getAllValueSets() as List<ValueSet>?

              if (valueSets != null) {
                  context.logger.info("Count of ValueSets:  ${valueSets.size}")
              }
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
               jedis.close()

    }
    }