package gov.cdc.dataexchange.client

import com.azure.cosmos.*
import com.azure.cosmos.models.CosmosBatch
import com.azure.cosmos.models.CosmosBulkOperations
import com.azure.cosmos.models.CosmosItemOperation
import com.azure.cosmos.models.PartitionKey
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory

/**
 * Class responsible for interacting with CosmosDB.
 * @Created - 10/11/2023
 * @Author QEH3@cdc.gov
 */
class CosmosDBClient {

    companion object {
        private val gson: Gson = GsonBuilder().serializeNulls().create()
        private val logger = LoggerFactory.getLogger(CosmosDBClient::class.java.simpleName)

        // singleton factory for connecting to cosmos
        object ConnectionFactory {
            val client: CosmosAsyncClient by lazy {
                CosmosClientBuilder()
                    .endpoint(System.getenv("CosmosConnectionString"))
                    .key(System.getenv("CosmosKey"))
                    .preferredRegions(listOf(System.getenv("cosmosRegion")))
                    .consistencyLevel(ConsistencyLevel.SESSION)
                    .gatewayMode()
                    .buildAsyncClient()
            }
            val database: CosmosAsyncDatabase by lazy {
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

        fun createAll(records: List<String>) {
            val batch: CosmosBatch = CosmosBatch.createCosmosBatch(PartitionKey("partitionKeyValue"))
            records.forEachIndexed { i: Int, record: String ->
                var recordMap: Map<String, Any?> = mutableMapOf()
                try {
                    val itemType = object : TypeToken<Map<String, Any?>>() {}.type
                    recordMap = gson.fromJson(record, itemType)
                } catch (e: Exception) {
                    logger.error("Error mapping record.")
                }
                val partitionKey = recordMap["message_uuid"]
                logger.info("[${i + 1}] record [message_uuid: $partitionKey] added to batch.")
                val op: CosmosItemOperation = CosmosBulkOperations.getCreateItemOperation(recordMap, PartitionKey(partitionKey))
                batch.createItemOperation(op)

            }
            logger.info("batch size = ${records.size}")
            ConnectionFactory.container.executeCosmosBatch(batch)
            logger.info("batch injested.")
        }
    }
}
