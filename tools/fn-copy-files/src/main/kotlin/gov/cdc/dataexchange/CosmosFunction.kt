import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.dex.azure.cosmos.CosmosClient
import gov.cdc.dex.azure.cosmos.PartKeyModifier
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.*

/**
 * Azure Functions with HTTP Trigger
 * Copy items from one Cosmos DB container to another.
 * Created: 11/07/2023
 * @author QEH3@cdc.gov
 */
class CosmosFunction {

    companion object {
        private val logger = LoggerFactory.getLogger(CosmosFunction::class.java.simpleName)
    }
    // not exposed - testing incomplete
//    @FunctionName("cosmos-copy")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "cosmos/copy/{fromDb}/{fromContainer}/{toDb}/{toContainer}"
        ) request: HttpRequestMessage<Optional<String>>,
        @BindingName("fromDb") fromDb: String,
        @BindingName("fromContainer") fromContainer: String,
        @BindingName("toDb") toDb: String,
        @BindingName("toContainer") toContainer: String,
    ): HttpResponseMessage {

        logger.info("CCOPY::HTTP trigger processed a request to copy items from $fromDb/$fromContainer to $toDb/$toContainer")

        // retrieve headers
        val sourceEndpoint = request.headers["source-endpoint"]
        val sourceKey = request.headers["source-key"]
        val destinationEndpoint = request.headers["destination-endpoint"] ?: sourceEndpoint
        val destinationKey = request.headers["destination-key"] ?: sourceKey
        val partitionKeyPath = request.headers["partition-key"] // This should be the path, like "/myPartitionKey"

        // validate
        if (sourceEndpoint.isNullOrEmpty() || sourceKey.isNullOrEmpty() || partitionKeyPath.isNullOrEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Missing or incorrect source Cosmos DB connection information or partition key in headers.")
                .build()
        }

        // initialize clients
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
            val itemsFlux = sourceClient.sqlReadItems("SELECT * FROM c", Map::class.java)
            itemsFlux.flatMap { item ->
                val partitionKeyValue = PartKeyModifier(partitionKeyPath).read(item as Map<String, Any>)
                if (partitionKeyValue == null) {
                    Mono.error<Throwable>(IllegalStateException("Partition Key not found for item: $item"))
                } else {
                    destClient.createItem(item, partitionKeyValue).thenReturn(item)
                }
            }
                .collectList()
                .block()
            sourceClient.closeClient()
            destClient.closeClient()
            return request.createResponseBuilder(HttpStatus.OK)
                .body("Successfully copied items from $fromDb/$fromContainer to $toDb/$toContainer")
                .build()
        } catch (e: IllegalStateException) {
            logger.error("IllegalStateException thrown: ${e.message}")
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Error occurred while processing items: ${e.message}")
                .build()
        } catch (e: Exception) {
            logger.error("Exception thrown: ${e.message}")
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error occurred while copying items: ${e.message}")
                .build()
        }
    }
}