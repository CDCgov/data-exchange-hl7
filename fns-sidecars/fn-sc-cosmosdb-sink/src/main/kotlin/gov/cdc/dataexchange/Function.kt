package gov.cdc.dataexchange

import gov.cdc.dex.azure.cosmos.CosmosClient
import com.azure.cosmos.models.CosmosBulkOperationResponse
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dataexchange.services.RecordService
import org.slf4j.LoggerFactory
import reactor.util.function.Tuple2

/**
 * Azure Function implementations.
 * @Created - 10/11/2023
 * @Author QEH3@cdc.gov
 */
class Function {

    companion object {
        private val ENDPOINT = System.getenv("CosmosEndpoint")
        private val KEY = System.getenv("CosmosKey")
        private val DATABASE_NAME = System.getenv("CosmosDBId")
        private val CONTAINER_NAME = System.getenv("CosmosContainerId")
        private val PARTITION_KEY_PATH = System.getenv("partitionKeyPath")

        private val logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        private var recordCount = 0
        private var totalRuntime: Long = 0

        val cosmosClient by lazy {
            CosmosClient(DATABASE_NAME, CONTAINER_NAME, ENDPOINT, KEY, PARTITION_KEY_PATH,
                isResponseOnWriteEnabled = false
            )
        }
    }

    /**
     * Synchronize the incoming messages from EventHub by upserting to CosmosDB.
     * @param records records found in eventhub
     */
    @FunctionName("cosmos-sink")
    fun cosmosSink(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%"
        )
        records: List<String>
    ) {
        val inputRecordCount = records.size
        logger.info("DB-SINK::Receiving $inputRecordCount records. ")
        try {
            println("PARTITION KEY PATH: \"${cosmosClient.getPartitionKeyPath()}\"")
            cosmosClient.bulkUpsert(RecordService.mapRecords(records))
                .index()
                .doOnNext { indexedBulkResponse: Tuple2<Long, CosmosBulkOperationResponse<Any>> -> // process each record
                    val index = indexedBulkResponse.t1
                    val response = indexedBulkResponse.t2.response
                    if (response != null) {
                        val duration = response.duration.toMillis()
                        totalRuntime += duration
                        val statusCode = response.statusCode
                        if (statusCode in 200..299) {
                            logger.info("DB-SINK::[${index + 1}] SUCCESS($statusCode) duration=${duration}ms")
                            recordCount++
                        } else {
                            logger.warn("DB-SINK::[${index + 1}] FAIL($statusCode) duration=${duration}ms")
                        }
                    } else {
                        logger.warn("DB-SINK::[${index + 1}] FAIL: No response found in execution")
                    }
                }
                .doOnComplete {
                    logger.info("DB-SINK::Total records: $recordCount in $totalRuntime ms")
                }
                .doOnError { error: Throwable -> logger.error("DB-SINK::Error during bulk upsert: ${error.message}") }
                .subscribe()
        } catch (e: IllegalStateException) {
            logger.error("DB-SINK::ERROR - make sure Cosmos Client, Database, and Container are properly initialized.\n${e.message}")
        } catch (e: IllegalArgumentException) {
            logger.error("DB-SINK::ERROR - ${e.message}")
        }
    }
}
