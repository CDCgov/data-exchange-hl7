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
        val client = VocabClient()
        context.logger.info("PhinVocabRead time trigger processed a request.")

        try {
            context.logger.info("STARTING VocabClient services")
            client.loadVocab()

        } catch (e: Exception) {
            context.logger.info("Failure in PhinVocabRead function : ${e.printStackTrace()} ")
            throw Exception("Failure in PhinVocabRead function ::${e.printStackTrace()}")
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
        client.loadLegacyMmgat(redisProxy)
        //load MMGAT's from API
        client.loadMMGAT(redisProxy)

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
        try {
            val client = EventCodeClient()
            context.logger.info("STARTING Event Code services")
            client.loadEventMaps(redisProxy)
            context.logger.info("Event Maps loaded")
            client.loadGroups()
            context.logger.info("Groups loaded")
            context.logger.info("COMPLETED Event Code services")
        } catch (e: Exception) {
            context.logger.info("Failure in EventCodesAndGroups function : ${e.printStackTrace()} ")
            throw Exception("Failure in EventCodesAndGroups function ::${e.printStackTrace()}")
        }

    }


}