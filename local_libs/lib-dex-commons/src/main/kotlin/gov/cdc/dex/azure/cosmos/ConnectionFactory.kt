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
class ConnectionFactory(private var cosmosClientConfig: CosmosClientConfig? = null) {

    val asyncCosmosClient: CosmosAsyncClient by lazy {
        if (cosmosClientConfig?.endpoint == null || cosmosClientConfig?.key == null) {
            throw IllegalStateException("ConnectionFactory not initialized.  No endpoint or key.")
        }
        if (cosmosClientConfig!!.directConnectionConfig == null && cosmosClientConfig!!.gatewayConnectionConfig != null) {
            CosmosClientBuilder()
                .endpoint(cosmosClientConfig!!.endpoint)
                .key(cosmosClientConfig!!.key)
                .preferredRegions(cosmosClientConfig!!.preferredRegions)
                .consistencyLevel(cosmosClientConfig!!.consistencyLevel)
                .gatewayMode(cosmosClientConfig!!.gatewayConnectionConfig)
                .contentResponseOnWriteEnabled(cosmosClientConfig!!.isResponseOnWriteEnabled)
                .throttlingRetryOptions(cosmosClientConfig!!.throttlingRetryOptions)
                .buildAsyncClient()
        } else {
            CosmosClientBuilder()
                .endpoint(cosmosClientConfig!!.endpoint)
                .key(cosmosClientConfig!!.key)
                .preferredRegions(cosmosClientConfig!!.preferredRegions)
                .consistencyLevel(cosmosClientConfig!!.consistencyLevel)
                .directMode(cosmosClientConfig!!.directConnectionConfig, cosmosClientConfig!!.gatewayConnectionConfig)
                .contentResponseOnWriteEnabled(cosmosClientConfig!!.isResponseOnWriteEnabled)
                .throttlingRetryOptions(cosmosClientConfig!!.throttlingRetryOptions)
                .buildAsyncClient()
        }

    }

    private val asyncDatabase: CosmosAsyncDatabase by lazy {
        if (cosmosClientConfig?.databaseName == null) {
            throw IllegalStateException("ConnectionFactory not initialized.  No database name.")
        }
        try {
            asyncCosmosClient.createDatabase(cosmosClientConfig!!.databaseName).block()
            asyncCosmosClient.getDatabase(cosmosClientConfig!!.databaseName)
        } catch (e: Exception) {
            asyncCosmosClient.getDatabase(cosmosClientConfig!!.databaseName)
        }
    }

    val asyncContainer: CosmosAsyncContainer by lazy {
        if (cosmosClientConfig?.containerName == null || cosmosClientConfig?.partitionKeyPath == null) {
            throw IllegalStateException("ConnectionFactory not initialized. No container name.")
        }
        try {
            asyncDatabase.createContainer(cosmosClientConfig!!.containerName, cosmosClientConfig!!.partitionKeyPath).block()
            asyncDatabase.getContainer(cosmosClientConfig!!.containerName)
        } catch (e: Exception) {
            asyncDatabase.getContainer(cosmosClientConfig!!.containerName)
        }
    }
}
