package gov.cdc.dex.mrr

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.TimerTrigger
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.util.StringUtils
import gov.cdc.vocab.service.bean.ValueSet
import gov.cdc.vocab.service.bean.ValueSetConcept
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import java.time.LocalDateTime
import java.util.*
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
    fun  runMmgat(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS)
        request: HttpRequestMessage<Optional<String>>,
        context : ExecutionContext): HttpResponseMessage {
//    fun runMmgat(
//        @TimerTrigger(name = "timerInfo", schedule = "%MMGAT_TIME_TRIGGER%") timerInfo: String?,
//        context: ExecutionContext
//    ) {

        context.logger.info("MMGATRead time trigger processed a request.")
        val redisName =  System.getenv("REDIS_CACHE_NAME")
        val redisKey = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(redisName, redisKey)
        //load legacy MMGAT'S
        MmgatClient().loadLegacyMmgat()
        this.loadMMGAT(redisProxy)
        return request.createResponseBuilder(HttpStatus.OK).body("Hello, ").build();
   }

    @FunctionName("EventCodesAndGroups")
    fun runEventCodesAndGroups(
        @TimerTrigger(name = "timerInfo", schedule = "%EVENT_CODES_TIME_TRIGGER%") timerInfo: String?,
        context: ExecutionContext
    ) {
        context.logger.info("Event Codes time trigger processed a request.")
        val redisName =  System.getenv("REDIS_CACHE_NAME")
        val redisKey = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(redisName, redisKey)
        try {
            val client = EventCodeClient()
            context.logger.info("STARTING Event Code services")
            client.loadEventMaps(redisProxy)
            context.logger.info("Event Maps loaded")
            client.loadGroups()
            context.logger.info("Groups loaded")
            context.logger.info("COMPLETED Event Code services")
        } catch(e:Exception){
            context.logger.info("Failure in EventCodesAndGroups function : ${e.printStackTrace()} ")
            throw Exception("Failure in EventCodesAndGroups function ::${e.printStackTrace()}")
        }

    }

    val logger = LoggerFactory.getLogger(TimerTriggerFunction::class.java.name)
    fun loadMMGAT(redisProxy: RedisProxy) {
            try {
                val mmgaClient = MmgatClient()
                println("STARTING MMGATRead services")

                val mmgaGuide = mmgaClient.getGuideAll().toString()

                val elem: JsonElement = JsonParser.parseString(mmgaGuide)
                //context.logger.info("Json Array size:" + elem.asJsonObject.getAsJsonArray("result").size())
                val mmgatJArray = elem.asJsonObject.getAsJsonArray("result")
                println("Json Array size:" + mmgatJArray.size())
                val gson = GsonBuilder().create()

                for (mmgatjson in mmgatJArray) {

                    val mj = mmgatjson.asJsonObject
                    //if (mj.get("guideStatus").asString.toLowerCase() in listOf(mmgaClient.GUIDANCE_STATUS_UAT, mmgaClient.GUIDANCE_STATUS_FINAL) )
                    if (mj.get("guideStatus").asString
                            .equals(mmgaClient.GUIDANCE_STATUS_UAT,true) || mj.get("guideStatus")
                            .asString.equals(mmgaClient.GUIDANCE_STATUS_FINAL,true)
                    ) {
                        val id = (mj.get("id").asString)
                        // context.logger.info("MMGAT id:$id")
                        val mGuide = mmgaClient.getGuideById(id)
                        val melement = JsonParser.parseString(mGuide.toString())
                        val mresult = melement.asJsonObject.get("result")

                        mresult.asJsonObject.remove("testScenarios")
                        mresult.asJsonObject.remove("testCaseScenarioWorksheetColumns")
                        mresult.asJsonObject.remove("columns")
                        mresult.asJsonObject.remove("templates")
                        mresult.asJsonObject.remove("valueSets")

                        val key = "mmg:"+ StringUtils.normalizeString(mj.get("name").asString)
                        print("MMGAT name: $key")
                        if (redisProxy.getJedisClient().exists(key))
                            redisProxy.getJedisClient().del(key)
                        try {
                            redisProxy.getJedisClient().set(key, gson.toJson(mresult))
                            println("...Done!")
                        } catch (e: Throwable) {
                            println("... ERRORED OUT")
                        }
                    }
                }
            } catch (e: Exception) {
                println("Failure in MMGATREAD function : ${e.printStackTrace()} ")
                throw e
            }
            println("MMGATREAD Function executed at: " + LocalDateTime.now())
            //return request.createResponseBuilder(HttpStatus.OK).body("Hello, ").build();
      //  }
    }


}