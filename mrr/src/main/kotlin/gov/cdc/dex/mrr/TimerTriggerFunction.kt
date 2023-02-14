package gov.cdc.dex.mrr


import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.TimerTrigger
import gov.cdc.dex.azure.RedisProxy

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
        val redisName = System.getenv("REDIS_CACHE_NAME")
        val redisKey = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(redisName, redisKey)
        val client = VocabClient(redisProxy)
        context.logger.info("PhinVocabRead time trigger processed a request.")

        try {
            context.logger.info("STARTING VocabClient services")
            client.loadVocab()

        } catch (e: Exception) {
            context.logger.info("Failure in PhinVocabRead function : ${e.message} ")
            throw Exception("Failure in PhinVocabRead function ::${e.message}")
        }
    }

    @FunctionName("MMGATRead")
    fun runMmgat(
        @TimerTrigger(name = "timerInfo", schedule = "%MMGAT_TIME_TRIGGER%") timerInfo: String?,
        context: ExecutionContext
    ) {

        context.logger.info("MMGATRead time trigger processed a request.")
        val redisName = System.getenv("REDIS_CACHE_NAME")
        val redisKey = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(redisName, redisKey)
        val client = MmgatClient()
         //load legacy MMGAT'S
        context.logger.info("STARTING MMG loader services")
        client.loadLegacyMmgat(redisProxy)
        context.logger.info("Legacy MMGs loaded")
        //load MMGAT's from API
        client.loadMMGAT(redisProxy)
        context.logger.info("MMGAT MMGs loaded")
        context.logger.info("COMPLETED MMG loader services")

    }

    @FunctionName("EventCodesAndGroups")
    fun runEventCodesAndGroups(
        @TimerTrigger(name = "timerInfo", schedule = "%EVENT_CODES_TIME_TRIGGER%") timerInfo: String?,
        context: ExecutionContext
    ) {
        context.logger.info("Event Codes time trigger processed a request.")
        val redisName = System.getenv("REDIS_CACHE_NAME")
        val redisKey = System.getenv("REDIS_CACHE_KEY")
        val redisProxy = RedisProxy(redisName, redisKey)
        val client = EventCodeClient()
        context.logger.info("STARTING Event Code services")
        client.loadEventMaps(redisProxy)
        context.logger.info("Event Maps loaded")
        client.loadGroups(redisProxy)
        context.logger.info("Groups loaded")
        context.logger.info("COMPLETED Event Code services")


    }


}