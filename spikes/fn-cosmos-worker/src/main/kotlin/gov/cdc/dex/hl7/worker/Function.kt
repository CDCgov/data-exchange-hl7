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
import com.fasterxml.jackson.databind.node.ObjectNode

class CosmosDBClient<T : Any>(private val cosmosClient: CosmosClient) {

    private val databaseName = System.getenv("CosmosDBId")
    private val containerName = System.getenv("CosmosContainer")

    // Insert row
    fun createItem(string: String ) {
        try {
            val database = cosmosClient.getDatabase(databaseName)
            val container = database.getContainer("receiverdebatcher")
            val objectMapper = ObjectMapper()
            val itemMap: Map<*, *> = objectMapper.readValue(string, Map::class.java)
            val itemResponse = container.createItem(itemMap)
            println("Created item: ${itemResponse}")
        } catch (e: Exception) {
            println("Error creating item: ${e.message}")
        }
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
        /*
          Receive Event Hub Message as a String, Convert to Object, Add id column
          Insert into CosmosDB.
        */
        try {
            // Use Gson - Parse String Message >> Add Id >>
            val inputEvent: JsonObject = JsonParser.parseString(message) as JsonObject
            inputEvent.addProperty("id", UUID.randomUUID().toString())
            // val objectMapper = ObjectMapper()
            //val inputEvent: ObjectNode = objectMapper.readValue(message, ObjectNode::class.java)
            // Add the "id" attribute
            // inputEvent.put("id", UUID.randomUUID().toString())
            // val jsonOutput = objectMapper.writeValueAsString(inputEvent)
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
            cosmosDB.createItem(inputEvent.toString())
            logger.info("Saved Message")
            return "Test"
        }  catch (e: Exception) {
            println("Error creating item: ${e.message}")
            return "FAILED TO parse"
        }
    }
    /*
    @FunctionName("cosmos-worker-eh")
    fun workerEH(
       @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            consumerGroup = "%EventHubConsumerGroup%",
            connection = "EventHubConnectionString")
       message: List<String?>): String? {
            message.forEachIndexed {
                messageIndex: Int, singleMessage: String? ->
                logger.info("message: $singleMessage")
                try {
                    val objectMapper = ObjectMapper()
                    val inputEvent: ObjectNode = objectMapper.readValue(singleMessage, ObjectNode::class.java)
                    // Add the "id" attribute
                    inputEvent.put("id", UUID.randomUUID().toString())
                    val jsonOutput = objectMapper.writeValueAsString(inputEvent)
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
                    if (jsonOutput != null) {
                        logger.info("response from Cosmos: Not Null Message")
                        cosmosDB.createItem(jsonOutput)
                        logger.info("Saved Message")
                    }
                    return "Test"
                }  catch (e: Exception) {
                    println("Error creating item: ${e.message}")
                    return "FAILED TO parse"
                }
        }
        return ""
    }
    */
}
