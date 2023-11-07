package gov.cdc.dataexchange

import com.microsoft.azure.functions.annotation.*
import com.microsoft.azure.functions.*
import java.util.*

/**
 * Azure Functions with HTTP Trigger to copy items from one Cosmos DB container to another.
 */
class Function {

    companion object {
        private val logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    }
    @FunctionName("copyItems")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.FUNCTION,
            route = "copy/{fromDb}/{fromContainer}/{toDb}/{toContainer}"
        ) request: HttpRequestMessage<Optional<String>>,
        @BindingName("fromDb") fromDb: String,
        @BindingName("fromContainer") fromContainer: String,
        @BindingName("toDb") toDb: String,
        @BindingName("toContainer") toContainer: String,
    ): HttpResponseMessage {

        logger.info("Kotlin HTTP trigger processed a request to copy items from $fromDb/$fromContainer to $toDb/$toContainer")

        // Retrieve source and destination endpoints and keys from headers
        val sourceEndpoint = request.headers["source-endpoint"]
        val sourceKey = request.headers["source-key"]
        val destinationEndpoint = request.headers["destination-endpoint"] ?: sourceEndpoint
        val destinationKey = request.headers["destination-key"] ?: sourceKey
        val partitionKeyPath = request.headers["partition-key"] // This should be the path, like "/myPartitionKey"

        // Validate source headers and partition key
        if (sourceEndpoint.isNullOrEmpty() || sourceKey.isNullOrEmpty() || partitionKeyPath.isNullOrEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Missing or incorrect source Cosmos DB connection information or partition key in headers.")
                .build()
        }

        // Initialize Cosmos clients for both source and destination containers
        val sourceClient = CosmosClient(
            databaseName = fromDb,
            containerName = fromContainer,
            endpoint = sourceEndpoint,
            key = sourceKey,
            partitionKeyPath = partitionKeyPath,
            isResponseOnWriteEnabled = true
        )

        val destClient = CosmosClient(
            databaseName = toDb,
            containerName = toContainer,
            endpoint = destinationEndpoint,
            key = destinationKey,
            partitionKeyPath = partitionKeyPath,
            isResponseOnWriteEnabled = true
        )

        try {
            // Read all items from the source container
            val items = sourceClient.sqlReadItems("SELECT * FROM c", Map::class.java).collectList().block()

            // Copy each item to the destination container
            items?.forEach { item ->
                destClient.createWithBlocking(item, PartitionKey(item[partitionKeyPath.substring(1)])) // Assume the partition key path starts with '/'
            }

            // Close the clients
            sourceClient.closeClient()
            destClient.closeClient()

            return request.createResponseBuilder(HttpStatus.OK)
                .body("Successfully copied items from $fromDb/$fromContainer to $toDb/$toContainer")
                .build()

        } catch (e: Exception) {
            logger.severe("Exception thrown: ${e.message}")
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error occurred while copying items: ${e.message}")
                .build()
        }
    }
}
