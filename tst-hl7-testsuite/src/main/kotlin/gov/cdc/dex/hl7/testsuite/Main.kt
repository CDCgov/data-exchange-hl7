package gov.cdc.dex.hl7.testsuite

import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.BlobContainerClient
import com.fasterxml.jackson.databind.JsonNode

import gov.cdc.dex.azure.cosmos.CosmosClient
import org.slf4j.Logger

import org.slf4j.LoggerFactory

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Companion{
    val cosmosDBName:String = System.getenv("COSMOS_DB_NAME")
    val cosmosDBKey:String = System.getenv("COSMOS_DB_KEY")
    val cosmosDBContainer:String = System.getenv("COSMOS_DB_CONTAINER_NAME")
    val cosmosDBEndpoint:String = System.getenv("COSMOS_DB_ENDPOINT")
    val cosmosDBPartitionKey:String = System.getenv("COSMOS_DB_PARTITION_KEY")
    val blobConnectionString:String = System.getenv("BLOB_INGEST_CONNECTION_STRING")
    val blobContainerName:String = System.getenv("BLOB_CONTAINER_NAME")

    val logger: Logger? = LoggerFactory.getLogger(Function::class.java.name)
    const val PATH_TO_MESSAGES = "src/main/resources/messages"
    private const val MESSAGE_TYPE: String = "message_type"
    private const val ROUTE: String= "route"
    private const val REPORTING_JURISDICTION= "reporting_jurisdiction"
    private const val ORIGINAL_FILE_NAME:String = "original_file_name"
    const val headerContentType = "text/plain"

    val uploadedBlobs: MutableSet<String> = mutableSetOf()

    val messagesMetadata = mapOf("case.txt" to mutableMapOf<String,String>(MESSAGE_TYPE to "CASE", ORIGINAL_FILE_NAME to "case.txt"),
        "test.hl7" to mutableMapOf<String,String>(MESSAGE_TYPE to "ELR", REPORTING_JURISDICTION to "48", ORIGINAL_FILE_NAME to "lab.hl7"),
        "Valid-PHLIP-ORU-DataType-DT.txt" to mutableMapOf<String,String>(MESSAGE_TYPE to "CASE", REPORTING_JURISDICTION to "48", ORIGINAL_FILE_NAME to "messages/case.txt"),
        "Valid-PHLIP-ORU-DataType-DTM.txt" to mutableMapOf<String,String>(MESSAGE_TYPE to "CASE", REPORTING_JURISDICTION to "48", ORIGINAL_FILE_NAME to "messages/case.txt"),
        "Valid-PHLIP-ORU-DataType-FT.txt" to mutableMapOf<String,String>(MESSAGE_TYPE to "CASE", REPORTING_JURISDICTION to "48", ORIGINAL_FILE_NAME to "messages/case.txt"),
        "Valid-PHLIP-ORU-DataType-TX.txt" to mutableMapOf<String,String>(MESSAGE_TYPE to "CASE", REPORTING_JURISDICTION to "48", ORIGINAL_FILE_NAME to "messages/case.txt"),
        "Valid-PHLIP-ORU-DataType-NM.txt" to mutableMapOf<String,String>(MESSAGE_TYPE to "CASE", REPORTING_JURISDICTION to "48", ORIGINAL_FILE_NAME to "messages/case.txt"),
        "Valid-PHLIP-ORU-DataType-SN.txt" to mutableMapOf<String,String>(MESSAGE_TYPE to "CASE", REPORTING_JURISDICTION to "48", ORIGINAL_FILE_NAME to "messages/case.txt")
        )


}
fun getCurrentDateTimeWithSeconds(): String {
    val currentDateTime = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    return currentDateTime.format(formatter)
}
fun addPayloadToTestResources(payloadAsJson: String, originalFileName:String){
    val testResourcesDirectory = "src/test/resources"
    val jsonFileWithPayload = File("$testResourcesDirectory/$originalFileName.json")
    jsonFileWithPayload.writeText(payloadAsJson)

}
fun dropMessagesToABlobStorage() {
    val blobServiceClient = BlobServiceClientBuilder()
        .connectionString(Companion.blobConnectionString)
        .buildClient()

    val containerClient:BlobContainerClient = blobServiceClient.getBlobContainerClient(Companion.blobContainerName)

    val directoryWithMessages = File(Companion.PATH_TO_MESSAGES)

    if (directoryWithMessages.exists()) {
        directoryWithMessages.listFiles()?.forEach {
            val blobNameWithTimeAppended = it.name + getCurrentDateTimeWithSeconds()
            val blobClient = containerClient.getBlobClient(blobNameWithTimeAppended)

            blobClient.upload(File(it.absolutePath).inputStream(),it.length(),true)

            blobClient.setMetadata(Companion.messagesMetadata[it.name])
            Companion.uploadedBlobs.add(blobNameWithTimeAppended)
        }
    }else{
        Companion.logger?.info("Check Messages under main/resources/messages.")
    }
}


fun queryCosmosDB(){
    val cosmosDBClient by lazy {
        CosmosClient(
            Companion.cosmosDBName,
            Companion.cosmosDBContainer,
            Companion.cosmosDBEndpoint,
            Companion.cosmosDBKey,
            Companion.cosmosDBPartitionKey
        )
    }

    for (blob in Companion.uploadedBlobs){
        val queryCosmosDBToRetrievePayload = "SELECT * FROM c WHERE c.metadata.provenance.ext_original_file_name=$blob"
        val queryItem = cosmosDBClient.sqlReadItems(queryCosmosDBToRetrievePayload,Map::class.java).blockLast()
        println (queryItem)
    }


}

fun main() {
    println("main")
    dropMessagesToABlobStorage()
    //queryCosmosDB()




}