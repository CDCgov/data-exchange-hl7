package gov.cdc.dex.azure.health

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
    fun checkDependency(dependency: AzureDependency, connectionString: String, target: String) : DependencyHealthData {
        val dependencyData = DependencyHealthData(dependency.description)
        try {
            when (dependency) {
                AzureDependency.EVENT_HUB -> checkEventHub(connectionString, target)
            }
        }
    }
    fun checkEventHub(connectionString: String, eventHubName: String) {
        val evHub = DedicatedEventHubSender(connectionString, eventHubName)
        evHub.disconnect()
    }

    fun checkServiceBus(connectionString: String, queueName: String) {
        val sbHub = ServiceBusProxy(connectionString, queueName)
        sbHub.disconnect()
    }

    fun checkStorageAccount(connectionString: String, containerName: String) {
        val blobProxy = AzureBlobProxy(connectionString, containerName)
        blobProxy.getAccountInfo()
    }

    fun checkCosmosDB(serviceEndpoint: String, key: String)  {
        val cosmosProxy = CosmosDBProxySimple(serviceEndpoint, key)
        cosmosProxy.disconnect()
    }
}