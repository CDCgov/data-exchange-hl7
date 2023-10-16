package gov.cdc.dataexchange.client

import com.azure.cosmos.*
import com.azure.cosmos.models.*
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.retry.Retry
import java.time.Duration

/**
 * Class responsible for interacting with CosmosDB.
 * @Created - 10/11/2023
 * @Author QEH3@cdc.gov
 */
class CosmosDBClient {

    companion object {
        private val logger = LoggerFactory.getLogger(CosmosDBClient::class.java.simpleName)
        private var recordCount = 0
        private var retryCount = 0
        private var runtime: Long = 0

        // singleton factory for connecting to cosmos
        object ConnectionFactory {
            val directConnectionConfig: DirectConnectionConfig = DirectConnectionConfig.getDefaultConfig().apply {
                connectTimeout = Duration.ofSeconds(5) // Setting the connection timeout
                idleConnectionTimeout = Duration.ofSeconds(60) // Setting the idle connection timeout
            }

            val client: CosmosAsyncClient by lazy {
                CosmosClientBuilder()
                    .endpoint(System.getenv("CosmosConnectionString"))
                    .key(System.getenv("CosmosKey"))
                    .preferredRegions(listOf(System.getenv("cosmosRegion")))
                    .consistencyLevel(ConsistencyLevel.EVENTUAL)
//                    .directMode(directConnectionConfig)
                    .gatewayMode()
                    .contentResponseOnWriteEnabled(false)
                    .buildAsyncClient()
            }
            private val database: CosmosAsyncDatabase by lazy {
                client.getDatabase(System.getenv("CosmosDBId"))
            }
            val container: CosmosAsyncContainer by lazy {
                database.getContainer(System.getenv("CosmosContainer"))
            }

            // close cosmos client on shutdown
            fun closeConnectionShutdownHook() {
                Runtime.getRuntime().addShutdownHook(Thread {
                    client.close()
                    logger.info("Closing Cosmos DB client...")
                })
            }
        }

        fun bulkUpsert(records: Flux<Map<String, Any>>, maxRetries: Long = 3): Mono<Void> {
            val startTime = System.currentTimeMillis()
            return records
                .index()
                .flatMap { indexedRecord ->
                    recordCount++
                    val record = indexedRecord.t2
                    val operation =
                        CosmosBulkOperations.getUpsertItemOperation(record, PartitionKey(record["message_uuid"]))
                    logger.info("[${indexedRecord.t1 + 1}] Record added to bulk operation: id=${record["id"]}")
                    Flux.just(operation)
                }
                .flatMap { operation ->
                    ConnectionFactory.container.executeBulkOperations<Any>(Flux.just(operation))
                        .onErrorResume { e ->
                            logger.error("ERROR EXECUTING OPERATION with partition key: ${operation.partitionKeyValue}", e)
                            Flux.empty() // Continue with other operations
                        }
                }
                .retryWhen(
                    Retry.max(maxRetries)
                        .doBeforeRetry { retrySignal ->
                            retryCount++
                            logger.info("RETRYING FAILED OPERATION [Attempt: ${retrySignal.totalRetries() + 1}]: $retrySignal")
                        }
                )
                .publishOn(Schedulers.boundedElastic())
                .doOnNext { response: CosmosBulkOperationResponse<Any> ->
                    if (response.response.statusCode != 200) {
                        logger.error("FAILED operation: Status code: ${response.response.statusCode}, record partitionKey=${response.operation.partitionKeyValue}")
                    }
                }
                .doOnComplete {
                    runtime += System.currentTimeMillis() - startTime
                    logger.info("COMPLETED $recordCount bulk upsert operations with $retryCount retries in ${runtime}ms .")
                }
                .doOnError { error -> logger.error("ERROR after maximum retries:\n${error.message}", error) }
                .then()
        }

    }
}
