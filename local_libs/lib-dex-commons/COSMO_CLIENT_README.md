# Cosmos Client
The Cosmos Client acts as a gateway, offering users seamless connectivity to a Cosmos DB container via the Cosmos DB
SDK. It leverages the <code>ConnectionFactory</code> class to establish a connection, providing a singleton instance of
the asynchronous Cosmos Client. This setup ensures a consistent connection to your specified database and container,
enabling the execution of CRUD (Create, Read, Update, Delete) operations.

### Dynamic Creation:
In scenarios where the specified database or container doesn't exist, the Cosmos Client is equipped with logic to
automatically create them. This feature ensures that your operations aren't halted due to the absence of the intended
database or container. The creation process respects configurations provided, ensuring any newly created entities match
the desired specifications.

### Configuration:
The client's configurability is a key feature, allowing for a customized setup through the
<code>CosmosClientConfig</code> class.  Essential parameters such as the Endpoint URL, Access Key, Database Name,
Container Name, and Partition Key Path must be specified. However,  the client also offers several optional 
configurations with sensible default values.

#### Configuration Parameters:
```kotlin
databaseName: Name of the database (Required)
containerName: Name of the container (Required)
endpoint: URL endpoint of your Cosmos DB instance (Required)
key: Access key of the Cosmos DB instance (Required)
partitionKeyPath: Path of the partition key (Required)
preferredRegions: List of preferred regions, defaulting to ["East US", "West US"]
consistencyLevel: Desired consistency level, effects performance (default = ConsistencyLevel.EVENTUAL)
isResponseOnWriteEnabled: Flag indicating whether to enable response on write; be mindful that enabling can impact performance (Default is false)
directConnectionConfig: Configuration of direct connection mode; the client will use Direct Mode over Gateway Mode (Optional)
```

### Example Usage with hardcode
Utilizing the asynchronous Cosmos Client requires integration with the Reactor library. Each method call
typically returns a ```Mono<T>```, where ```T``` often corresponds to ```CosmosItemResponse<T>```. In our use case,
```T``` maps to the type ```Map<String, Any>```, which is used to map JSON records. To evaluate or execute the
operations contained within a Mono, one must invoke either ```.subscribe()``` or ```.block()```. This is evident in the
read and write functions demonstrated below:

* note: if ```isResponseOnWriteEnabled = true```, the ```CosmosItemResponse<T>.item``` would return the item in the response.
```kotlin
class myClass {
    companion object {
		private const val DATABASE_NAME = "myDatabase"
		private const val CONTAINER_NAME = "myContainer"
		private const val ENDPOINT = "https://myCosmosAccount.documents.azure.com:443/"
		private const val KEY = "<REDACTED>"
		private const val PARTKEY_PATH = "/event/partition_key"
    }
	
    private lateinit var cosmosClient: CosmosClient
    
    init {
        cosmosClient = CosmosClient(DATABASE_NAME, CONTAINER_NAME, ENDPOINT, KEY, PARTKEY_PATH)
    }

	private fun read(itemId: String, partitionKey: PartitionKey): Map<String, Any>
			= cosmosClient.readItem(itemId, partitionKey, Map::class.java).block()!!.item as Map<String, Any>
	private fun write(item: Map<String, Any>) = cosmosClient.upsertItem(item)
		.map {response -> response.item }
		.subscribe()
	
	fun myFunction(item: Map<String, Any>) {
        try {
            write(item)
		} finally {
		    cosmosClient.closeCosmos()
		}
    }
}
```
Here I am using the read and write blocking functions, which blocks the thread until the operation is complete
```kotlin
private fun read(itemId: String, partitionKey: PartitionKey): Map<String, Any>
	= cosmosClient.readWithBlocking(itemId, partitionKey, Map::class.java) as Map<String, Any>
private fun write(item: Map<String, Any>): Map<String, Any>? = cosmosClient.upsertWithBlocking(item)

fun myFunction(item: Map<String, Any>) {
	try {
		write(item)
	} finally {
		cosmosClient.closeCosmos()
	}
}
```

## Implementation:
The <code>CosmosClient class</code>, found in the gov.cdc.dex.azure.cosmos package, is your primary gateway to interact
with Azure Cosmos DB.

### Key Initializations:
During its instantiation, <code>CosmosClient</code> leverages the supplied <code>CosmosClientConfig</code> to initialize
the ConnectionFactory. It then retrieves singleton instances of both the asynchronous Cosmos Client and Cosmos Container.

### Supported Operations:
<code>CosmosClient</code> aids in performing a range of crucial operations on a Cosmos DB instance:

- <code>Bulk Create:</code> Enables asynchronous create of multiple records utilizing Reactor Flux.
- <code>Bulk Upsert:</code> Enables asynchronous upsert of multiple records utilizing Reactor Flux.
- <code>Item Creation:</code> Adds a new record to the designated container.
- <code>Item Reading:</code> Fetches an item using its unique ID and associated partition key.
- <code>Item Upsert:</code> Introduces a fresh item or substitutes an existing one within the container.
- <code>Item Update:</code> Replaces a current item inside the container.
- <code>Item Deletion:</code> Excises an item from the container.
- <code>SQL Query Execution:</code> Performs SQL queries for data retrieval. Note: This function is strictly read-only.

### Error Management:
Every operation comes with protective measures to handle cases where the <code>CosmosClient</code> hasn't been initialized. In such instances, an IllegalStateException is raised. This precaution guarantees that all interactions with the database are executed within a securely initialized environment.

### Cleanup:
Post the execution of singular transactions or queries, the client concludes the connection to liberate system resources.
