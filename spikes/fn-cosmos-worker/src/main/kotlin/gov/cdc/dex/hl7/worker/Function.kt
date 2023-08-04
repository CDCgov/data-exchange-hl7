package gov.cdc.dex.hl7.worker

import com.azure.cosmos.*
import com.azure.cosmos.models.CosmosItemResponse
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.util.CosmosPagedIterable
import com.azure.messaging.eventhubs.*
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.metadata.*
import org.slf4j.LoggerFactory
import java.io.*
import java.time.Duration
import java.util.*
import kotlin.streams.toList

data class Person(val id: String, val name: String, val age: Int)

class CosmosDBClient<T : Any>(private val cosmosClient: CosmosClient) {

    private val databaseName = System.getenv("CosmosDBId")
    private val containerName = System.getenv("CosmosContainer")

    // Insert row
    fun createItem(item: T ) {
        val database = cosmosClient.getDatabase(databaseName)
        val container = database.getContainer(containerName)
        val itemResponse = container.createItem(item)
    }

    // Read Row
    fun readItem(itemId: String, mappingClass: Class<T>): CosmosItemResponse<T>? {
        try{
            val database = cosmosClient.getDatabase(databaseName)
            println("IN DB: $databaseName")
            val container = database.getContainer(containerName)
            println("IN DContinaer: $containerName")

            val partitionKey = PartitionKey(itemId)
            println("IN Partition Key $partitionKey")

            val itemResponse: CosmosItemResponse<T> = container.readItem(
                itemId, partitionKey, mappingClass
            )

            val requestCharge: Double = itemResponse.getRequestCharge()
            val requestLatency: Duration = itemResponse.getDuration()
            println("Item successfully ${itemResponse.getItem()}, $requestCharge, $requestLatency")
            println("readItem Response(): $itemResponse")
            return itemResponse
        } catch (e: Exception) {
            println("Error readItem(): ${e.message}")
            return null
        }
    }

    // Run Query
    fun queryItems(mappingClass: Class<T>): List<T>? {
        try{
            val database = cosmosClient.getDatabase(databaseName)
            val container = database.getContainer(containerName)
            kotlin.io.println("I have a container and DB")

            val query = "SELECT * FROM c"
            val options = CosmosQueryRequestOptions()

            val queryIterator: CosmosPagedIterable<T> =
                container.queryItems(query, options, mappingClass)
            kotlin.io.println("I ran a query")

            return queryIterator.stream().toList()
        }catch (e: Exception) {
            println("Error readItem(): ${e.message}")
            return null
        }

    }
}

class Function {

    companion object {
        // Replace with your Cosmos DB account settings
        val cosmosEndpoint = System.getenv("CosmosConnectionString")!!
        val cosmosKey = System.getenv("CosmosKey")!!
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        private var client: CosmosClient? = null

        private val database: CosmosDatabase? = null
        private val container: CosmosContainer? = null

        fun close() {
            client!!.close()
        }
    }

    @FunctionName("cosmos-worker")
    fun worker(
        @HttpTrigger(name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS)
        request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext): String {
    /*        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            consumerGroup = "%EventHubConsumerGroup%",
            connection = "EventHubConnectionString")
        message: String?): String? { */

        logger.info("Start Cosmos Client")
        val preferredRegions = ArrayList<String>()
        preferredRegions.add("East US")
        // Initialize Cosmos client
        val cosmosClient: CosmosClient = CosmosClientBuilder()
            .endpoint(cosmosEndpoint)
            .key(cosmosKey)
            .consistencyLevel(ConsistencyLevel.EVENTUAL)
            .directMode()
            .buildClient()

        //  Create sync client

        logger.info("defined Cosmos Client")
        val cosmosDB = CosmosDBClient<Any>(cosmosClient)

        val response = cosmosDB.readItem("1", Any::class.java)
        logger.info("response from Cosmos: ${response}")

        val response2 = cosmosDB.queryItems(Any::class.java)
        logger.info("response from Cosmos: ${response2}")

        return "Test"
    }
}