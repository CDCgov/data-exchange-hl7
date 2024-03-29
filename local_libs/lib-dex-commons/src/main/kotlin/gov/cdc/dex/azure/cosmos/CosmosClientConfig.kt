package gov.cdc.dex.azure.cosmos

import com.azure.cosmos.ConsistencyLevel
import com.azure.cosmos.DirectConnectionConfig

/**
 * Cosmos Client Config Model
 * @author QEH3@cdc.gov
 */
data class CosmosClientConfig(
    var databaseName: String? = null,
    var containerName: String? = null,
    var endpoint: String? = null,
    var key: String? = null,
    var partitionKeyPath: String? = null,
    var preferredRegions: List<String>? = null,
    var consistencyLevel: ConsistencyLevel? = null,
    var isResponseOnWriteEnabled: Boolean = false,
    var directConnectionConfig: DirectConnectionConfig? = null
)