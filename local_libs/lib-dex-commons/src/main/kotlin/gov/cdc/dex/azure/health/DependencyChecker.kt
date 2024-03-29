package gov.cdc.dex.azure.health

import com.azure.core.amqp.AmqpRetryOptions
import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosClientBuilder
import com.azure.messaging.eventhubs.EventHubClientBuilder
import com.azure.messaging.eventhubs.EventHubProducerClient
import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusReceiverClient
import com.azure.storage.blob.BlobServiceClientBuilder

class DependencyChecker {
    enum class AzureDependency(val description: String) {
        EVENT_HUB("Event Hub"),
        SERVICE_BUS("Service Bus"),
        STORAGE_ACCOUNT("Storage Account"),
        COSMOS_DB("Cosmos DB")
    }
    private fun checkDependency(dependency: AzureDependency, action: () -> Any) : DependencyHealthData {
        val data = DependencyHealthData(dependency.description)
        try {
            action()
            data.status = "UP"
        } catch (e: Exception) {
            data.status = "DOWN"
            data.healthIssues = "${e.message}"
        }
        return data
    }
    fun checkEventHub(connectionString: String, eventHubName: String) : DependencyHealthData {
        return checkDependency(AzureDependency.EVENT_HUB) {
            val producer: EventHubProducerClient = EventHubClientBuilder()
                .connectionString(connectionString, eventHubName)
                .retryOptions(AmqpRetryOptions().setMaxRetries(1))
                .buildProducerClient()
            producer.partitionIds.map { it -> it.toString() }
            producer.close()
        }
    }

    fun checkServiceBus(connectionString: String, queueName: String) : DependencyHealthData {
        return checkDependency(AzureDependency.SERVICE_BUS) {
            val serviceBusClient: ServiceBusReceiverClient = ServiceBusClientBuilder()
                .connectionString(connectionString)
                .receiver()
                .queueName(queueName)
                .buildClient()
            serviceBusClient.peekMessage()
            serviceBusClient.close()
        }
    }

    fun checkStorageAccount(connectionString: String, containerName: String): DependencyHealthData {
        return checkDependency(AzureDependency.STORAGE_ACCOUNT) {
            val blobServiceClient = BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
            val containerClient = blobServiceClient.getBlobContainerClient(containerName)
            containerClient.properties
        }
    }

    fun checkCosmosDB(serviceEndpoint: String, key: String, database: String, container: String) : DependencyHealthData {
        return checkDependency(AzureDependency.COSMOS_DB) {
            val cosmosClient: CosmosClient = CosmosClientBuilder()
                .endpoint(serviceEndpoint)
                .key(key)
                .buildClient()
            val db = cosmosClient.getDatabase(database)
            db.getContainer(container).readThroughput()
            cosmosClient.close()
        }
    }
}