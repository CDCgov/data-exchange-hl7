package gov.cdc.dex.azure.health

import com.azure.core.amqp.AmqpRetryMode
import com.azure.core.amqp.AmqpRetryOptions
import com.azure.cosmos.CosmosClient
import com.azure.cosmos.CosmosClientBuilder
import com.azure.messaging.eventhubs.EventHubClientBuilder
import com.azure.messaging.eventhubs.EventHubProducerClient
import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusReceiverClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.queue.QueueClientBuilder
import gov.cdc.dex.azure.DedicatedEventHubSender
import java.time.Duration

class DependencyChecker {
    enum class AzureDependency(val description: String) {
        EVENT_HUB("Event Hub"),
        SERVICE_BUS("Service Bus"),
        STORAGE_ACCOUNT("Storage Account"),
        STORAGE_QUEUE("Storage Queue"),
        COSMOS_DB("Cosmos DB")
    }
    val retryOptions = AmqpRetryOptions()
        .setMaxRetries(0)
        .setMode(AmqpRetryMode.FIXED)
        .setTryTimeout(Duration.ofSeconds(10))

    private fun checkDependency(dependency: AzureDependency, resourceName: String, action: () -> Any) : DependencyHealthData {
        val data = DependencyHealthData(dependency.description, resourceName=resourceName)
        try {
            action()
            data.status = "UP"
        } catch (e: Exception) {
            data.status = "DOWN"
            data.healthIssues = "${e.message}"
        }
        return data
    }
    fun checkEventHub(eventHubClient: DedicatedEventHubSender) : DependencyHealthData {
        return checkDependency(AzureDependency.EVENT_HUB, eventHubClient.getEventHubName()) {
            eventHubClient.getPartitionIds()
        }
    }

    fun checkEventHub(connectionString: String, eventHubName: String) : DependencyHealthData {
        return checkDependency(AzureDependency.EVENT_HUB, eventHubName) {
            val client: EventHubProducerClient = EventHubClientBuilder()
                .connectionString(connectionString, eventHubName)
                .retryOptions(retryOptions)
                .buildProducerClient()
            client.partitionIds.count()
            client.close()
        }
    }

    fun checkServiceBusQueue(connectionString: String, queueName: String) : DependencyHealthData {
        return checkDependency(AzureDependency.SERVICE_BUS, queueName) {
            val serviceBusClient: ServiceBusReceiverClient = ServiceBusClientBuilder()
                .retryOptions(retryOptions)
                .connectionString(connectionString)
                .receiver()
                .queueName(queueName)
                .buildClient()
            serviceBusClient.peekMessage()
            serviceBusClient.close()
        }
    }

    fun checkStorageAccount(connectionString: String, containerName: String): DependencyHealthData {
        return checkDependency(AzureDependency.STORAGE_ACCOUNT, getAccountName(connectionString)) {
            val blobServiceClient = BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
            val containerClient = blobServiceClient.getBlobContainerClient(containerName)
            containerClient.properties
        }
    }

    fun checkStorageAccount(connectionString: String): DependencyHealthData {
        return checkDependency(AzureDependency.STORAGE_ACCOUNT, getAccountName(connectionString)) {
            val blobServiceClient = BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient()
            blobServiceClient.properties
        }
    }

    fun checkStorageQueue(connectionString: String, queueName: String) : DependencyHealthData {
        return checkDependency(AzureDependency.STORAGE_QUEUE, queueName) {
            val queueClient = QueueClientBuilder()
                .connectionString(connectionString)
                .queueName(queueName)
                .buildClient()
            queueClient.properties
        }
    }

    fun checkCosmosDB(serviceEndpoint: String, key: String, database: String, container: String) : DependencyHealthData {
        return checkDependency(AzureDependency.COSMOS_DB, database) {
            val cosmosClient: CosmosClient = CosmosClientBuilder()
                .endpoint(serviceEndpoint)
                .key(key)
                .buildClient()
            val db = cosmosClient.getDatabase(database)
            db.getContainer(container).readThroughput()
            cosmosClient.close()
        }
    }

    private fun getAccountName(connectionString: String) : String {
        return connectionString.substringAfter("AccountName=").substringBefore(";")
    }
}