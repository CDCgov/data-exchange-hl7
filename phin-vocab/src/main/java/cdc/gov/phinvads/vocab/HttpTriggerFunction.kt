package cdc.gov.phinvads.vocab

import com.google.gson.GsonBuilder
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.vocab.service.bean.ValueSet
import gov.cdc.vocab.service.bean.ValueSetConcept
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis
import java.util.*
import kotlin.concurrent.thread

/**
 * Azure Functions with HTTP Trigger.
 */
class HttpTriggerFunction {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("VocabReadFunction")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        ) request: HttpRequestMessage<Optional<String?>?>,
        context: ExecutionContext
    ): HttpResponseMessage {
        try {
           thread {
               context.logger.info("Java HTTP trigger processed a request.")
               var redisCacheName = System.getenv("REDISCACHEHOSTNAME")
               var rediscachekey = System.getenv("REDISCACHEKEY")
               // redisCacheName = "temedehl7.redis.cache.windows.net"
               // rediscachekey ="1ofYheE07YlzGOuDAauSQwQ09tj5u4fVrAzCaKAsQt0="
               println("cacheHostname :${redisCacheName} ")

               // Connect to the Azure Cache for Redis over the TLS/SSL port using the key.
               val jedis = Jedis(
                   redisCacheName, 6380, DefaultJedisClientConfig.builder()
                       .password(rediscachekey)
                       .ssl(true)
                       .build()
               )
               // Simple PING command
               println( "\nCache Command  : Ping" )
               println( "Cache Response : " + jedis.ping())
               //println( "Cache Response : " + jedis.set("Message", "Test"))

               val client = VocabClient()
               println("STARTING VocabClient services")
               val valueSets = client.getAllValueSets() as List<ValueSet>?
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
               println("END OF VocabClient services")
               jedis.close()

           }.start()
        } catch (re:Exception ) {
           println("exception in thread:  $re");
        }

        return request.createResponseBuilder(HttpStatus.OK).body("phinvads function execution started in the background").build()

    }
}