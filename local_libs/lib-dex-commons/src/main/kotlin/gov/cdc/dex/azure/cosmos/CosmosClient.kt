package gov.cdc.dex.azure.cosmos

import com.azure.cosmos.*
import com.azure.cosmos.models.*
import gov.cdc.dex.util.PartKeyModifier
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.retry.Retry

/**
 * Cosmos client - using ConnectionFactory initialized with CosmosClientConfig provides CRUD, upsert,
 * and bulk operations.
 * @param databaseName required
 * @param containerName required
 * @param endpoint required
 * @param key required
 * @param partitionKeyPath default "/message_uuid"
 * @param preferredRegions default "East US", "West US"
 * @param consistencyLevel default EVENTUAL
 * @param isResponseOnWriteEnabled default false, expect performance loss if set to true
 * @param directConnectionConfig default null, if provided, client will connect using directMode
 * @since 10/26/2023
 * @author QEH3@cdc.gov
 */
class CosmosClient(
    private val databaseName: String?,
    private val containerName: String?,
    private val endpoint: String?,
    private val key: String?,
    private val partitionKeyPath: String?,
    private val preferredRegions: List<String> = mutableListOf("East US", "West US"),
    private val consistencyLevel: ConsistencyLevel = ConsistencyLevel.EVENTUAL,
    private val isResponseOnWriteEnabled: Boolean = false,
    private val directConnectionConfig: DirectConnectionConfig? = null
) {

    companion object {
        private val logger = LoggerFactory.getLogger(CosmosClient::class.java.simpleName)
        private var cosmosAsyncClient: CosmosAsyncClient? = null
        private var cosmosContainer: CosmosAsyncContainer? = null
    }

    init {
        if (containerName.isNullOrBlank() || databaseName.isNullOrBlank() || endpoint.isNullOrBlank() || key.isNullOrBlank()
            || partitionKeyPath.isNullOrBlank() || endpoint.substring(0,8) != ("https://") || key.takeLast(2) != "==") {
            throw IllegalStateException("Unable to build Cosmos Client.  Check config.")
        }
        val cosmosClientConfig = CosmosClientConfig(databaseName, containerName, endpoint, key, partitionKeyPath,
            preferredRegions, consistencyLevel, isResponseOnWriteEnabled, directConnectionConfig)
        ConnectionFactory.init(cosmosClientConfig)
        try {
            // singleton async cosmos client
            cosmosAsyncClient = ConnectionFactory.asyncCosmosClient
        } catch (e: IllegalStateException) {
            logger.error("Unable to build Cosmos Client.  " +
                    "ConnectionFactory must be initialized with a cosmosClientConfig." +
                    "\nFAIL: ${e.message}")
            throw e
        }
        try {
            // singleton async cosmos container object
            cosmosContainer = ConnectionFactory.asyncContainer
        } catch (e: IllegalStateException) {
            logger.error("Unable to connect with container.  " +
                    "ConnectionFactory must be initialized with a valid database and container." +
                    "\nFAIL: ${e.message}")
            throw e
        }
    }

    fun getEndpoint() = endpoint
    fun getDatabaseName() = databaseName
    fun getContainerName() = containerName
    fun getPartitionKeyPath() = partitionKeyPath!!
    fun closeCosmos() = cosmosAsyncClient?.close()

    /**
     * Generic blocking CRUD functions
     */
    @Throws(IllegalStateException::class)
    inline fun <reified T> createWithBlocking(item: T, partitionKey: PartitionKey): T? =
        createItem(item, partitionKey).block()?.item

    @Throws(IllegalStateException::class)
    inline fun <reified T> createWithBlocking(item: T): T? =
        createItem(item).block()?.item

    @Throws(IllegalStateException::class)
    inline fun <reified T> readWithBlocking(itemId: String, partitionKey: PartitionKey, itemType: Class<T>): T? =
        readItem(itemId, partitionKey, itemType).block()?.item

    @Throws(IllegalStateException::class)
    inline fun <reified T> updateWithBlocking(item: T, itemId: String, partitionKey: PartitionKey): T? =
        updateItem(item, itemId, partitionKey).block()?.item

    @Throws(IllegalStateException::class)
    inline fun <reified T> upsertWithBlocking(item: T): T? =
        upsertItem(item).block()?.item

    @Throws(IllegalStateException::class)
    inline fun <reified T> upsertWithBlocking(item: T, partitionKey: PartitionKey): T? =
        upsertItem(item, partitionKey).block()?.item

    @Throws(IllegalStateException::class)
    fun deleteWithBlocking(itemId: String, partitionKey: PartitionKey): Any? =
        deleteItem(itemId, partitionKey).block()?.item

    /**
     * Private helper function to execute bulk operations found in Flux of CosmosItemOperation.
     *
     * **Note:** you must subscribe or block the resulting Flux to begin async processing.
     * @param operations required
     * @param maxRetries default = 3
     * @return Flux<CosmosBulkOperationResponse<Any>>
     */
    private fun bulkExecute(operations: Flux<CosmosItemOperation>, maxRetries: Long = 3)
            : Flux<CosmosBulkOperationResponse<Any>> {
        return try {
            operations.flatMap { operation ->
                cosmosContainer!!.executeBulkOperations<Any>(Flux.just(operation))
                    .onErrorResume { e ->
                        logger.error("ERROR EXECUTING OPERATION with partition key: ${operation.partitionKeyValue}" +
                                "\nerror message: ${e.message}")
                        Flux.empty() // Continue with other operations
                    }
            }
                .retryWhen(
                    Retry.max(maxRetries)
                        .doBeforeRetry { retrySignal ->
                            logger.info("RETRYING FAILED OPERATION [Attempt: ${retrySignal.totalRetries() + 1}]: $retrySignal")
                        }
                )
                .publishOn(Schedulers.boundedElastic())
                .doOnNext { response: CosmosBulkOperationResponse<Any> ->
                    if (response.response.statusCode !in 200..299) {
                        logger.error("FAILED operation: Status code: ${response.response.statusCode}, record partitionKey=${response.operation.partitionKeyValue}")
                    }
                }
                .doOnComplete { }
                .doOnError { error -> logger.error("ERROR after maximum retries:\n${error.message}") }
        } catch (e: Exception) {
            logger.error("Error on bulk execute: ${e.message}")
            Flux.empty()
        } finally {
            closeCosmos() // close cosmos client each time it completes a bulk operation
        }
    }

    /**
     * For each item in the items list, it creates bulk operations of create items
     *
     * **Note:** not generic.  Items in list must be mapped to Map<String, Any>
     *
     * **Note:** you must subscribe or block the resulting Mono to begin async processing.
     * @param items list of mapped items to Map<String, Any>
     * @return Mono<Void>
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    fun bulkCreate(items: List<Map<String, Any>>): Mono<Void> {
        if(items.isEmpty()) throw IllegalArgumentException("Missing items.")
        if(cosmosAsyncClient == null || cosmosContainer == null || partitionKeyPath == null) {
            throw IllegalStateException("Unable to bulk upsert items.  CosmosClient not initialized")
        }

        val operationFlux: Flux<CosmosItemOperation> = Flux.fromIterable(items)
            .index()
            .flatMap { indexedItem ->
                val item: Map<String, Any> = indexedItem.t2
                val partitionKey = PartKeyModifier(partitionKeyPath).read(item)
                val operation = CosmosBulkOperations.getCreateItemOperation(item, partitionKey)
                logger.info("[${indexedItem.t1 + 1}] Record added to bulk operation: message_uuid=${item["message_uuid"]}")
                Flux.just(operation)
            }
        return bulkExecute(operationFlux).then()
    }

    /**
     * For each item in the items list, it creates bulk operations of upsert items.
     *
     * **Note:** not generic.  Items in list must be mapped to Map<String, Any>
     *
     * **Note:** you must subscribe or block the resulting Mono to begin async processing.
     * @param items list of mapped items to Map<String, Any>
     * @return Mono<Void>
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun bulkUpsert(items: List<Map<String, Any>>): Mono<Void> {
        if(items.isEmpty()) throw IllegalArgumentException("Missing items.")
        if(cosmosAsyncClient == null || cosmosContainer == null || partitionKeyPath == null) {
            throw IllegalStateException("Unable to bulk upsert items.  CosmosClient not initialized")
        }

        val operationFlux: Flux<CosmosItemOperation> = Flux.fromIterable(items)
            .index()
            .flatMap { indexedItem ->
                val item: Map<String, Any> = indexedItem.t2
                val partitionKey = PartKeyModifier(partitionKeyPath).read(item)
                val operation = CosmosBulkOperations.getUpsertItemOperation(item, partitionKey)
                logger.info("[${indexedItem.t1 + 1}] Record added to bulk operation: message_uuid=${item["message_uuid"]}")
                Flux.just(operation)
            }
        return bulkExecute(operationFlux).then()
    }

    /**
     * Creates a new generic item in the container, given its partition key.  Closes connection.  If item exists, it
     * will not overwrite.
     *
     * **Note:** you must subscribe or block the resulting Mono to begin async processing and get the response.
     * @param item
     * @param partitionKey
     * @return Mono<Void>
     */
    @Throws(IllegalStateException::class, Exception::class)
    fun <T> createItem(item: T, partitionKey: PartitionKey): Mono<CosmosItemResponse<T>> {
        if(cosmosContainer == null) {
            throw IllegalStateException("Unable to create item.  CosmosClient not initialized")
        }
        return try {
            cosmosContainer!!.createItem(item, partitionKey, null)
        } finally {
            closeCosmos()
        }
    }

    /**
     * Creates a new generic item in the container.  Closes connection.  If item exists, it will not overwrite.
     *
     * **Note:** you must subscribe or block the resulting Mono to begin async processing and get the response.
     * @param item
     * @return Mono<CosmosItemResponse<T>>
     */
    @Throws(IllegalStateException::class, Exception::class)
    fun <T> createItem(item: T): Mono<CosmosItemResponse<T>> {
        if(cosmosContainer == null) throw IllegalStateException("Unable to create item.  CosmosClient not initialized")

        return try {
            cosmosContainer!!.createItem(item)
        } finally {
            closeCosmos()
        }
    }

    /**
     * read item from Cosmos container, given id and partition key.
     *
     * **Note:** you must subscribe or block the resulting Mono to begin async processing and get the response.
     * @param id
     * @param partitionKey
     * @param itemType
     * @return Mono<CosmosItemResponse<T>>
     */
    @Throws(IllegalStateException::class)
    fun <T> readItem(id: String, partitionKey: PartitionKey, itemType: Class<T>): Mono<CosmosItemResponse<T>> {
        if(cosmosContainer == null) throw IllegalStateException("Unable to read item.  CosmosClient not initialized")

        logger.info("READING item: id=$id, partitionKey=$partitionKey")
        return try {
            cosmosContainer!!.readItem(id, partitionKey, itemType)
        } finally {
            closeCosmos()
        }
    }

    /**
     *
     * **Note:** you must subscribe or block the resulting Flux to begin async processing and get the response.
     * @param query
     * @param itemType result item class type
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun <T> sqlReadItems(query: String, itemType: Class<T>): Flux<T> {
        if (query.isBlank()) throw IllegalArgumentException("Check usage.  Provide a query string. ")
        if (cosmosContainer == null) throw IllegalStateException("Unable to execute bulk read. CosmosClient not initialized")

        logger.info("EXECUTING BULK READ SQL query: $query")
        val querySpec = SqlQuerySpec(query)
        return try {
            cosmosContainer!!.queryItems(querySpec, CosmosQueryRequestOptions(), itemType)
                .onErrorMap { error ->
                    logger.error("Error while executing bulk read SQL query: $query. ${error.message}")
                    error
                }
        } finally {
            closeCosmos()
        }
    }

    @Throws(IllegalStateException::class)
    fun <T> upsertItem(item: T): Mono<CosmosItemResponse<T>> {
        if(cosmosContainer == null) {
            throw IllegalStateException("Unable to create item.  CosmosClient not initialized")
        }
        logger.info("UPSERTING item: $item")
        return try {
            cosmosContainer!!.upsertItem(item)
        } finally {
            closeCosmos()
        }
    }

    @Throws(IllegalStateException::class)
    fun <T> upsertItem(item: T, partitionKey: PartitionKey): Mono<CosmosItemResponse<T>> {
        if(cosmosContainer == null) {
            throw IllegalStateException("Unable to upsert item.  CosmosClient not initialized")
        }
        logger.info("UPSERTING item: partitionKey=$partitionKey")
        return try {
            cosmosContainer!!.upsertItem(item, partitionKey, CosmosItemRequestOptions())
        } finally {
            closeCosmos()
        }
    }

    @Throws(IllegalStateException::class)
    fun <T> updateItem(item: T, id: String, partitionKey: PartitionKey): Mono<CosmosItemResponse<T>> {
        if(cosmosContainer == null) {
            throw IllegalStateException("Unable to update item.  CosmosClient not initialized")
        }
        logger.info("UPDATING item: id=$id, partitionKey=$partitionKey")
        return try {
            cosmosContainer!!.replaceItem(item, id, partitionKey)
        } finally {
            closeCosmos()
        }
    }

    @Throws(IllegalStateException::class)
    fun deleteItem(id: String, partitionKey: PartitionKey): Mono<CosmosItemResponse<Any>> {
        if(cosmosContainer == null) {
            throw IllegalStateException("Unable to delete item.  CosmosClient not initialized")
        }
        logger.info("DELETING item: id=$id, partiotionKey=$partitionKey")
        return try {
            cosmosContainer!!.deleteItem(id, partitionKey)
        } finally {
            closeCosmos()
        }
    }

}