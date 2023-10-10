package gov.cdc.dataexchange.client

import com.azure.cosmos.*
import com.azure.cosmos.models.CosmosItemRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import org.slf4j.LoggerFactory

/**
 * Class responsible for interacting with CosmosDB.
 */
class CosmosDBClient {

    companion object {
        private val gson: Gson = GsonBuilder().serializeNulls().create()
        private val logger = LoggerFactory.getLogger(CosmosDBClient::class.java.simpleName)

        // singleton factory for connecting to cosmos
        object ConnectionFactory {
            private val client: CosmosClient by lazy {
                CosmosClientBuilder()
                    .endpoint(System.getenv("CosmosConnectionString"))
                    .key(System.getenv("CosmosKey"))
                    .preferredRegions(listOf(System.getenv("cosmosRegion")))
                    .consistencyLevel(ConsistencyLevel.SESSION)
                    .gatewayMode()
                    .buildClient()
            }
            private val database: CosmosDatabase by lazy {
                client.getDatabase(
                    System.getenv("CosmosDBId"))
            }
            val container: CosmosContainer by lazy {
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

        // Read an item provided id and partition key
        fun read(recordId: String, partitionKey: String): String? {
            logger.info("Reading record [id=$recordId; partitionKey=$partitionKey]...")
            var response: String? = null
            try {
                val cosmosItemResponse = ConnectionFactory.container.readItem(recordId, PartitionKey(partitionKey), Map::class.java)
                response = gson.toJson(cosmosItemResponse?.item)
                logger.info("response record: $response")
            } catch(e: Exception) {
                logger.error("Unable to read record [id=$recordId]")
            }
            return response
        }

        // Delete a record provided id and partition key
        fun delete(recordId: String, partitionKey: String): Boolean {
            return try {
                logger.info("Deleting record [id=$recordId; partitionKey=$partitionKey]")
                val requestOptions = CosmosItemRequestOptions()
                ConnectionFactory.container.deleteItem(recordId, PartitionKey(partitionKey), requestOptions)
                true
            } catch (e: Exception) {
                logger.error("Unable to delete record [id=$recordId; partitionKey=$partitionKey]")
                false
            }
        }

        // Upsert an item provided as a JsonObject.
        fun upsert(item: JsonObject): Boolean {
            return try {
                val itemMap: Map<String, Any?> = gson.fromJson(item, object : TypeToken<Map<String, Any?>>() {}.type)
                logger.info("Upserting record...")
                ConnectionFactory.container.upsertItem(itemMap)
                logger.info("Record upserted")
                true
            } catch (e: Exception) {
                logger.error("Error inserting record.")
                false
            }
        }
    }
}
