package gov.cdc.dex.hl7.worker

import com.azure.messaging.eventhubs.*
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.*
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*
import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.CosmosDatabase
import com.azure.cosmos.models.CosmosItemResponse

class Function {

    companion object {
        // Replace with your Cosmos DB account settings
        val cosmosEndpoint = "your-cosmos-endpoint-url"
        val cosmosKey = "your-cosmos-key"
        val databaseId = "your-database-id"
        val containerId = "your-container-id"
        // private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    }

    @FunctionName("cosmos-worker")
    fun worker(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            consumerGroup = "%EventHubConsumerGroup%",
            connection = "EventHubConnectionString")
        message: String?): String? {

        // logger.info("Triggering Cosmos Worker")
        println("Message: $message")
        // Initialize Cosmos client
        val cosmosClient: CosmosClient = CosmosClientBuilder()
            .endpoint(cosmosEndpoint)
            .key(cosmosKey)
            .buildClient()

        // Get or create the database
        val cosmosDatabase: CosmosDatabase = cosmosClient.getDatabase(databaseId)

        return "Test"
    }


}