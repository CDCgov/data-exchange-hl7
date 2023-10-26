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
object ConnectionFactory {

    private var cosmosClientConfig: CosmosClientConfig? = null

    fun init(cosmosClientConfig: CosmosClientConfig) {
        ConnectionFactory.cosmosClientConfig = cosmosClientConfig
    }

    val asyncCosmosClient: CosmosAsyncClient by lazy {
        if (cosmosClientConfig?.endpoint == null || cosmosClientConfig?.key == null) {
            throw IllegalStateException("CosmosConnFactory not initialized.  No endpoint or key.")
        }
        if (cosmosClientConfig?.directConnectionConfig == null) {
            CosmosClientBuilder()
                .endpoint(cosmosClientConfig!!.endpoint)
                .key(cosmosClientConfig!!.key)
                .preferredRegions(cosmosClientConfig!!.preferredRegions)
                .consistencyLevel(cosmosClientConfig!!.consistencyLevel)
                .gatewayMode()
                .contentResponseOnWriteEnabled(cosmosClientConfig!!.isResponseOnWriteEnabled)
                .buildAsyncClient()
        } else {
            CosmosClientBuilder()
                .endpoint(cosmosClientConfig!!.endpoint)
                .key(cosmosClientConfig!!.key)
                .preferredRegions(cosmosClientConfig!!.preferredRegions)
                .consistencyLevel(cosmosClientConfig!!.consistencyLevel)
                .directMode(cosmosClientConfig!!.directConnectionConfig)
                .contentResponseOnWriteEnabled(cosmosClientConfig!!.isResponseOnWriteEnabled)
                .buildAsyncClient()
        }

    }

    private val asyncDatabase: CosmosAsyncDatabase by lazy {
        if (cosmosClientConfig?.databaseName == null) {
            throw IllegalStateException("CosmosConnFactory not initialized.  No database name.")
        }
        try {
            asyncCosmosClient.createDatabase(cosmosClientConfig!!.databaseName).block()
            asyncCosmosClient.getDatabase(cosmosClientConfig!!.databaseName)
        } catch (e: Exception) {
            asyncCosmosClient.getDatabase(cosmosClientConfig!!.databaseName)
        }
    }

    val asyncContainer: CosmosAsyncContainer by lazy {
        if (cosmosClientConfig?.containerName == null) {
            throw IllegalStateException("CosmosConnFactory not initialized. No container name.")
        }
        try {
            asyncDatabase.createContainer(cosmosClientConfig!!.containerName, cosmosClientConfig!!.partitionKeyPath).block()
            asyncDatabase.getContainer(cosmosClientConfig!!.containerName)
        } catch (e: Exception) {
            asyncDatabase.getContainer(cosmosClientConfig!!.containerName)
        }
    }
}
