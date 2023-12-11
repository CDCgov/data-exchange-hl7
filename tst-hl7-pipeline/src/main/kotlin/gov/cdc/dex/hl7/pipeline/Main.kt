package gov.cdc.dex.hl7.pipeline

import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.BlobContainerClient

import gov.cdc.dex.azure.cosmos.CosmosClient
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class PipelineTest {
    companion object {
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        val cosmosDBName: String = System.getenv("COSMOS_DB_NAME")
        val cosmosDBKey: String = System.getenv("COSMOS_DB_KEY")
        val cosmosDBContainer: String = System.getenv("COSMOS_DB_CONTAINER_NAME")
        val cosmosDBEndpoint: String = System.getenv("COSMOS_DB_ENDPOINT")
        val cosmosDBPartitionKey: String = System.getenv("COSMOS_DB_PARTITION_KEY")
        val blobConnectionString: String = System.getenv("BLOB_INGEST_CONNECTION_STRING")
        val blobContainerName: String = System.getenv("BLOB_CONTAINER_NAME")

        const val PATH_TO_MESSAGES = "src/main/resources/messages"
        private const val MESSAGE_TYPE: String = "message_type"
        private const val ROUTE: String = "route"
        private const val REPORTING_JURISDICTION = "reporting_jurisdiction"
        private const val ORIGINAL_FILE_NAME: String = "original_file_name"
        const val HEADER_CONTENT_TYPE = "text/plain"

        val uploadedBlobs: MutableSet<String> = mutableSetOf()

        val messagesMetadata = mapOf(
            "case.txt" to mutableMapOf<String, String>(MESSAGE_TYPE to "CASE", ORIGINAL_FILE_NAME to "case.txt"),
            "test.hl7" to mutableMapOf<String, String>(
                MESSAGE_TYPE to "ELR",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "lab.hl7"
            ),
            "Valid-PHLIP-ORU-DataType-DT.txt" to mutableMapOf<String, String>(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "messages/case.txt"
            ),
            "Valid-PHLIP-ORU-DataType-DTM.txt" to mutableMapOf<String, String>(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "messages/case.txt"
            ),
            "Valid-PHLIP-ORU-DataType-FT.txt" to mutableMapOf<String, String>(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "messages/case.txt"
            ),
            "Valid-PHLIP-ORU-DataType-TX.txt" to mutableMapOf<String, String>(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "messages/case.txt"
            ),
            "Valid-PHLIP-ORU-DataType-NM.txt" to mutableMapOf<String, String>(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "messages/case.txt"
            ),
            "Valid-PHLIP-ORU-DataType-SN.txt" to mutableMapOf<String, String>(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "messages/case.txt"
            )
        )
    }

    fun getCurrentDateTimeWithSeconds(): String {
        return try {
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            currentDateTime.format(formatter)
        } catch (e: DateTimeParseException) {
            println("${e.message}")
            "Error: Unable to retrieve date and time"
        }

    }

    fun addPayloadToTestResources(payloadAsJson: String, originalFileName: String) {
        val testResourcesDirectory = "src/test/resources"
        try {
            val jsonFileWithPayload = File("$testResourcesDirectory/$originalFileName.json")
            jsonFileWithPayload.writeText(payloadAsJson)

        }catch (e:Exception) {
            logger.error("Error occurred while copying payload to $testResourcesDirectory")
        }


    }

    fun dropMessagesToABlobStorage() {
        try {
            val blobServiceClient = BlobServiceClientBuilder()
                .connectionString(blobConnectionString)
                .buildClient()

            val containerClient: BlobContainerClient = blobServiceClient.getBlobContainerClient(blobContainerName)

            val directoryWithMessages = File(PATH_TO_MESSAGES)

            if (directoryWithMessages.exists()) {
                directoryWithMessages.listFiles()?.forEach {
                    val blobNameWithTimeAppended = it.name + getCurrentDateTimeWithSeconds()
                    val blobClient = containerClient.getBlobClient(blobNameWithTimeAppended)

                    blobClient.upload(File(it.absolutePath).inputStream(), it.length(), true)

                    blobClient.setMetadata(messagesMetadata[it.name])
                    uploadedBlobs.add(blobNameWithTimeAppended)
                }
            } else {
                throw FileNotFoundException("HL7 messages not found under $PATH_TO_MESSAGES")
            }
        } catch (e:Exception) {
            logger.error("Error occurred while uploading message to a blob storage: ${e.message}")
        }
    }


    fun queryCosmosDB() {
        try {
            val cosmosDBClient by lazy {
                CosmosClient(
                    cosmosDBName,
                    cosmosDBContainer,
                    cosmosDBEndpoint,
                    cosmosDBKey,
                    cosmosDBPartitionKey
                )
            }

            for (blob in uploadedBlobs) {
                val queryCosmosDBToRetrievePayload =
                    "SELECT * FROM c WHERE c.metadata.provenance.ext_original_file_name=$blob"
                val queryItem = cosmosDBClient.sqlReadItems(queryCosmosDBToRetrievePayload, Map::class.java).blockLast()
                //TO DO - extract payload and call addPayloadToTestResources()  to copy   to test/resources
                println (queryItem)

            }
        }catch (e: Exception) {
            logger.error("Error occurred while querying Cosmos DB: ${e.message}")

        }
    }


}
fun main() {
    println("main")
    val pipeline = PipelineTest()
    pipeline.dropMessagesToABlobStorage()
    //queryCosmosDB()



}