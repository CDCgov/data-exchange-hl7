package gov.cdc.dex.azure.health

import com.azure.core.amqp.AmqpRetryOptions
import com.azure.messaging.eventhubs.EventHubClientBuilder
import com.azure.messaging.eventhubs.EventHubProducerClient
import gov.cdc.dex.azure.AzureBlobProxy
import gov.cdc.dex.azure.CosmosDBProxySimple
import gov.cdc.dex.azure.DedicatedEventHubSender
import gov.cdc.dex.azure.ServiceBusProxy

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
            val sbHub = ServiceBusProxy(connectionString, queueName)
            // need to do something to initiate connection
            sbHub.disconnect()
        }
    }

    fun checkStorageAccount(connectionString: String, containerName: String): DependencyHealthData {
        return checkDependency(AzureDependency.STORAGE_ACCOUNT) {
            val blobProxy = AzureBlobProxy(connectionString, containerName)
            blobProxy.getAccountInfo()
        }
    }

    fun checkCosmosDB(serviceEndpoint: String, key: String, database: String, container: String) : DependencyHealthData {
        return checkDependency(AzureDependency.COSMOS_DB) {
            val cosmosProxy = CosmosDBProxySimple(serviceEndpoint, key)
            // need to do something to initiate connection
            cosmosProxy.disconnect()
        }
    }
}