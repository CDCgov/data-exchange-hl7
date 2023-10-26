package gov.cdc.dex.hl7.testsuite

import com.azure.cosmos.ConsistencyLevel
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.models.SqlParameter
import com.azure.cosmos.models.SqlQuerySpec
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.azure.storage.blob.models.BlobStorageException

import org.slf4j.LoggerFactory
import java.io.File


object Companion{
    val cosmosDBName:String = System.getenv("COSMOS_DB_NAME")
    val cosmosDBKey:String = System.getenv("COSMOS_DB_KEY")
    val cosmosDBConnectionString:String = System.getenv("COSMOS_DB_CONNECTION_STRING")
    val cosmosDBContainer:String = System.getenv("COSMOS_DB_CONTAINER_NAME")

    val blobConnectionString:String = System.getenv("BLOB_INGEST_CONNECTION_STRING")
    val blobContainerName:String = System.getenv("BLOB_CONTAINER_NAME")

    val hl7MessagesPath = "src/main/resources"


    val logger = LoggerFactory.getLogger(Function::class.java.name)
}



fun dropMessagesToBlobStorage() {
    /*
    uploads messages  to a blob storage container
     */
    val blobServiceClient = BlobServiceClientBuilder()
        .connectionString(Companion.blobConnectionString)
        .buildClient()

    val containerClient:BlobContainerClient = blobServiceClient.getBlobContainerClient(Companion.blobContainerName)

    val directory = File(Companion.hl7MessagesPath)
    if (directory.exists()) {
        directory.listFiles()?.forEach {
            val blobClient = containerClient.getBlobClient(it.name)
            blobClient.upload(File(it.absolutePath).inputStream(),it.length())
        }
    }else{
        println ("Check Resources directory.")
    }
}


fun queryCosmosDB(message_uuid:String){
    /*
    queries cosmos db and returns the payload

    val cosmosClient =CosmosClientBuilder()
        .endpoint(Companion.cosmosDBConnectionString)
        .key(Companion.cosmosDBKey)
        .consistencyLevel(ConsistencyLevel.SESSION)
        .buildClient()

    val database = cosmosClient.getDatabase(Companion.cosmosDBName)
    val container = database.getContainer(Companion.cosmosDBContainer)

    */
}


fun comparePayloads(){
    /*

     */
}
fun main() {
    print("main")
    dropMessagesToBlobStorage()



}