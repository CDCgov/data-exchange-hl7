# Cosmos Client
The Cosmos Client acts as a gateway, offering users seamless connectivity to a Cosmos DB container via the Cosmos DB SDK. It leverages the `ConnectionFactory` class to establish a connection, providing a singleton instance of the asynchronous Cosmos Client. This setup ensures a consistent connection to your specified database and container, enabling the execution of CRUD (Create, Read, Update, Delete) operations.

---

### Dynamic Creation
In scenarios where the specified database or container doesn't exist, the Cosmos Client is equipped with logic to automatically create them. This feature ensures that your operations aren't halted due to the absence of the intended database or container. The creation process respects configurations provided, ensuring any newly created entities match the desired specifications.

---

### Configuration

The client's configurability is a key feature, allowing for a customized setup through the [`CosmosClientConfig`](#cosmosclientconfig-class) class. Essential parameters such as the Endpoint URL, Access Key, Database Name, Container Name, and Partition Key Path must be specified. However, the client also offers several optional configurations with sensible default values.

#### Configuration Parameters

- **databaseName**: Name of the database (Required)
- **containerName**: Name of the container (Required)
- **endpoint**: URL endpoint of your Cosmos DB instance (Required)
- **key**: Access key of the Cosmos DB instance (Required)
- **partitionKeyPath**: Path of the partition key (Required)
- **preferredRegions**: List of preferred regions, defaulting to ["East US", "West US"]
- **consistencyLevel**: Desired consistency level, affects performance (default = `ConsistencyLevel.SESSION`)
- **isResponseOnWriteEnabled**: Flag indicating whether to enable response on write; be mindful that disabling this can improve performance (Default is true)
- **[directConnectionConfig](https://azuresdkdocs.blob.core.windows.net/$web/java/azure-cosmos/4.51.0/com/azure/cosmos/DirectConnectionConfig.html)**: Configuration of direct connection mode; if provided, the client will use Direct Mode over Gateway Mode (Optional)

#### Example directConnectionConfig

```kotlin
private val directConnectionConfig = DirectConnectionConfig()
        .setConnectTimeout(Duration.ofSeconds(5)) // default
        .setIdleConnectionTimeout(Duration.ZERO) // default - means indefinate
        .setIdleEndpointTimeout(Duration.ofSeconds(70)) // default
        .setMaxConnectionsPerEndpoint(30) // default
        .setMaxRequestsPerConnection(10) // default
```

---

## Implementation
The `CosmosClient` class, found in the gov.cdc.dex.azure.cosmos package, is the primary gateway to interact with Azure Cosmos DB.

### Example Usage with hardcode
Utilizing the asynchronous Cosmos Client requires integration with the Reactor library. Each method call typically returns a `Mono<T>` or `Flux<T>`, where `T` often corresponds to `CosmosItemResponse<T>` (`T` being the type written or the type provided in a read operation) In this example, `T` is the type `Map<String, Any>`, which is used to map JSON records. To evaluate or execute the operations contained within a Reactor type, one must invoke either `.subscribe()` or `.block()`. This is evident in the read and write functions demonstrated below:

```kotlin
class myClass {
    companion object {
        private const val DATABASE_NAME = "myDatabase"
        private const val CONTAINER_NAME = "myContainer"
        private const val ENDPOINT = "https://myCosmosAccount.documents.azure.com:443/"
        private const val KEY = "<REDACTED>"
        private const val PARTITION_KEY_PATH = "/event/partition_key"
    }

    private val cosmosClient by lazy {
        CosmosClient(DATABASE_NAME, CONTAINER_NAME, ENDPOINT, KEY, PARTITION_KEY_PATH)
    }

    fun read(itemId: String, partitionKey: PartitionKey): Map<String, Any>
            = cosmosClient.readItem(itemId, partitionKey, Map::class.java).block()?.item as Map<String, Any>

    fun write(item: Map<String, Any>) = cosmosClient.upsertItem(item).subscribe()

    fun myFunction(id: String, partKey: PartitionKey) {
        val item: Map<String, Any> = read(id, partKey)
        // perform transformations to item...
        write(item)
    }
}
```
Here is an example using the read and write blocking functions, which blocks the thread until the operation is complete, effectively making it synchronous.
```kotlin
private fun read(itemId: String, partitionKey: PartitionKey): Map<String, Any>
	= cosmosClient.readWithBlocking(itemId, partitionKey, Map::class.java) as Map<String, Any>

private fun write(item: Map<String, Any>): Map<String, Any>? = cosmosClient.upsertWithBlocking(item)

fun myFunction(id: String, partKey: PartitionKey) {
    val item: Map<String, Any> = read(id, partKey)
    // perform transformations to item...
    write(item)
}
```

---

### Supported Operations
- `Item Creation` Adds a new record to the designated container.
- `Item Reading` Fetches an item using its unique ID and associated partition key.
- `Item Upsert` Introduces a fresh item or substitutes an existing one within the container.
- `Item Update` Replaces a current item inside the container.
- `Item Deletion` Excises an item from the container.
- `SQL Query Execution` Performs SQL queries for data retrieval. Note: This function is strictly read-only.
- `Bulk Execute` Executes asynchronous bulk operations
- `Bulk Create` Enables asynchronous create of multiple records utilizing Reactor Flux.
- `Bulk Upsert` Enables asynchronous upsert of multiple records utilizing Reactor Flux.

---

### Error Management

The `CosmosClient` ensures robust operations by throwing an `IllegalArgumentException` for initialization errors, such as missing or incorrect parameters. If database operations are attempted before the client is properly initialized, an `IllegalStateException` is raised to prevent execution in an unstable state.

During bulk operations, the client handles errors gracefully, logging issues and continuing with subsequent operations. It employs a retry mechanism, with logs for each retry, and logs a final error if all retries fail, ensuring persistent operation despite individual failures.

---

### Cleanup

The `CosmosClient` class is designed to maintain open connections for repeated use, which is beneficial for performance due to reduced overhead in establishing connections. However, for scenarios where the resources need to be explicitly released or during application shutdown, the class provides a `closeClient()` function. Invoking this method will close the client and its associated resources, ensuring that all connections are properly terminated and system resources are freed. This can be particularly useful in resource-constrained environments or to adhere to best practices in resource management.