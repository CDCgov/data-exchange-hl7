package gov.cdc.dex.azure

import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.CosmosClient

class CosmosDBProxySimple (serviceEndpoint: String, key: String) {
    private val cosmosClient: CosmosClient = CosmosClientBuilder()
        .endpoint(serviceEndpoint)
        .key(key)
        .buildClient()
    fun disconnect() {
        cosmosClient.close()
    }
}