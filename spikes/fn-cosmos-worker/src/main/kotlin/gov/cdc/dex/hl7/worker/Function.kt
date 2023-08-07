package gov.cdc.dex.hl7.worker

import com.azure.cosmos.*
import com.azure.cosmos.models.CosmosItemResponse
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.util.CosmosPagedIterable
import com.azure.messaging.eventhubs.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
import com.fasterxml.jackson.databind.ObjectMapper

class CosmosDBClient<T : Any>(private val cosmosClient: CosmosClient) {

    private val databaseName = System.getenv("CosmosDBId")
    private val containerName = System.getenv("CosmosContainer")

    // Insert row
    fun createItem(string: String ) {
        try {
            val database = cosmosClient.getDatabase(databaseName)
            val container = database.getContainer("debatcher")

            val objectMapper = ObjectMapper()
            val itemMap: Map<*, *> = objectMapper.readValue(string, Map::class.java)
            val item = objectMapper.readValue(string, Map::class.java)

            // Generate a unique ID if "id" is not present in the JSON
            val id = itemMap["id"] ?: UUID.randomUUID().toString()
            itemMap.toMutableMap()["id"] = id
            println("This is the ID: $id")
            println("Check out this Map $itemMap")
            println("Check out this other MAP $item")

            val itemResponse = container.createItem(itemMap)
            println("Created item: ${itemResponse}")
        } catch (e: Exception) {
            println("Error creating item: ${e.message}")
        }
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
        val message = request.body?.get().toString()
        logger.info("message: $message")
        try {
            val inputEvent: JsonObject = JsonParser.parseString(message) as JsonObject
            logger.info("inputEvent: $inputEvent")

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
            logger.info("Write Cosmos Client")
            cosmosDB.createItem(message)
            logger.info("Saved Message")

            return "Test"
        }  catch (e: Exception) {
            println("Error creating item: ${e.message}")
            return "FAILED TO parse"
        }

    }

    @FunctionName("cosmos-worker-eh")
    fun worker(
       @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            consumerGroup = "%EventHubConsumerGroup%",
            connection = "EventHubConnectionString")
        message: String?): String? {

        logger.info("message: $message")
        try {
            val inputEvent: JsonObject = JsonParser.parseString(message) as JsonObject
            logger.info("inputEvent: $inputEvent")

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
            logger.info("Write Cosmos Client")
            if (message != null) {
                logger.info("response from Cosmos: Not Null Message")
                cosmosDB.createItem(message)
                logger.info("Saved Message")
            }
            return "Test"
        }  catch (e: Exception) {
            println("Error creating item: ${e.message}")
            return "FAILED TO parse"
        }
    }
}