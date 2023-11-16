package gov.cdc.dex.azure.cosmos

import com.azure.cosmos.CosmosAsyncClient
import com.azure.cosmos.CosmosAsyncContainer
import com.azure.cosmos.CosmosAsyncDatabase
import com.azure.cosmos.CosmosClientBuilder

/**
 * Singleton implementation of CosmosConnFactory
 * created 10/23/2023
 * Author: QEH3@cdc.gov
 */
class ConnectionFactory(cosmosClientConfig: CosmosClientConfig) {

    val asyncCosmosClient: CosmosAsyncClient by lazy {
        if (cosmosClientConfig.endpoint == null || cosmosClientConfig.key == null) {
            throw IllegalStateException("ConnectionFactory not initialized.  No endpoint or key.")
        }
        println("INITIALIZING COSMOS CLIENT...")
        if (cosmosClientConfig.directConnectionConfig == null) {
            CosmosClientBuilder()
                .endpoint(cosmosClientConfig.endpoint)
                .key(cosmosClientConfig.key)
                .preferredRegions(cosmosClientConfig.preferredRegions)
                .consistencyLevel(cosmosClientConfig.consistencyLevel)
                .gatewayMode()
                .contentResponseOnWriteEnabled(cosmosClientConfig.isResponseOnWriteEnabled)
                .buildAsyncClient()
        } else {
            CosmosClientBuilder()
                .endpoint(cosmosClientConfig.endpoint)
                .key(cosmosClientConfig.key)
                .preferredRegions(cosmosClientConfig.preferredRegions)
                .consistencyLevel(cosmosClientConfig.consistencyLevel)
                .directMode(cosmosClientConfig.directConnectionConfig)
                .contentResponseOnWriteEnabled(cosmosClientConfig.isResponseOnWriteEnabled)
                .buildAsyncClient()
        }

    }

    private val asyncDatabase: CosmosAsyncDatabase by lazy {
        if (cosmosClientConfig.databaseName == null) {
            throw IllegalStateException("ConnectionFactory not initialized.  No database name.")
        }
        println("INITIALIZING DATABASE...")
        try {
            asyncCosmosClient.createDatabase(cosmosClientConfig.databaseName).block()
            asyncCosmosClient.getDatabase(cosmosClientConfig.databaseName)
        } catch (e: Exception) {
            asyncCosmosClient.getDatabase(cosmosClientConfig.databaseName)
        }
    }

    val asyncContainer: CosmosAsyncContainer by lazy {
        if (cosmosClientConfig.containerName == null || cosmosClientConfig.partitionKeyPath == null) {
            throw IllegalStateException("ConnectionFactory not initialized. No container name.")
        }
        println("INITIALIZING CONTAINER...")
        try {
            asyncDatabase.createContainer(cosmosClientConfig.containerName, cosmosClientConfig.partitionKeyPath).block()
            asyncDatabase.getContainer(cosmosClientConfig.containerName)
        } catch (e: Exception) {
            asyncDatabase.getContainer(cosmosClientConfig.containerName)
        }
    }
}
