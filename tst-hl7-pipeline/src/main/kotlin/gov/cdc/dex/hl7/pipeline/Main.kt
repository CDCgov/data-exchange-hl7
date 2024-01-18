package gov.cdc.dex.hl7.pipeline

import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.BlobContainerClient
import gov.cdc.dex.azure.cosmos.CosmosClient
import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.net.URLEncoder

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class PipelineTest {
    companion object {
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        val cosmosDBName: String = System.getenv("COSMOS_DB_NAME")
        val cosmosDBKey: String = System.getenv("COSMOS_DB_KEY")
        val cosmosDBEndpoint: String = System.getenv("COSMOS_DB_ENDPOINT")
        val cosmosDBPartitionKey: String = System.getenv("COSMOS_DB_PARTITION_KEY")
        val blobConnectionString: String = System.getenv("BLOB_INGEST_CONNECTION_STRING")
        val blobContainerName: String = System.getenv("BLOB_CONTAINER_NAME")
        //val receiverDebatcherContainerName: String = System.getenv("RECEIVER_DEBATCHER")
       // val redactorContainerName: String = System.getenv("REDACTOR")
        val structureValidatorContainerName: String = System.getenv("STRUCTURE_VALIDATOR")
        //val jsonLakeContainerName: String = System.getenv("JSON_LAKE")
        //val lakeOfSegmentsContainerName: String = System.getenv("LAKE_OF_SEGMENTS")

        const val PATH_TO_MESSAGES = "src/main/resources/messages"
        private const val MESSAGE_TYPE = "message_type"
        private const val ROUTE = "route"
        private const val REPORTING_JURISDICTION:String = "reporting_jurisdiction"
        private const val ORIGINAL_FILE_NAME:String = "original_file_name"

        val uploadedBlobs: MutableSet<String> = mutableSetOf()

    }


    private fun buildMetadata(uniqueTimeStamp: String): Map<String, MutableMap<String, out String?>> {
        return mapOf(
            "PHLIP_FLU_2.5.1_PID5_ERROR.txt" to mutableMapOf<String, String?>(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_FLU_2.5.1_PID5_ERROR.txt",
                ROUTE to "PHLIP_FLU",
                REPORTING_JURISDICTION to "48"
            ),
            "PHLIP_FLU_2.5.1_VALID.txt" to mutableMapOf<String, String?>(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_FLU_2.5.1_VALID.txt",
                ROUTE to "PHLIP_FLU",
                REPORTING_JURISDICTION to "48"
            ),
            "PHLIP_FLU_2.5.1_NO_MSH3.txt" to mutableMapOf<String, String?>(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_FLU_2.5.1_NO_MSH3.txt",
                ROUTE to "PHLIP_FLU",
                REPORTING_JURISDICTION to "48"
            )

        )
    }

    private fun getCurrentDateTimeWithSeconds(): String {
        return try {
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            currentDateTime.format(formatter)
        } catch (e: DateTimeParseException) {
            println("${e.message}")
            "DEX::tst-hl7-pipeline Unable to retrieve date and time"
        }

    }
     private fun addPayloadToTestResources(payloadAsJson: String, originalFileName: String) {
        val encodedOriginalFileName = URLEncoder.encode(originalFileName.replace(".txt",".json"),"UTF-8").replace("%20","_")
        val testResourcesDirectory = "src/test/resources/new-payloads/$encodedOriginalFileName"
        try {
            val jsonFileWithPayload = File(testResourcesDirectory)

            if (!jsonFileWithPayload.exists()) {
                println (payloadAsJson)
                jsonFileWithPayload.writeText(payloadAsJson)

            }
        }catch (e:FileNotFoundException) {
            logger.error("DEX::tst-hl7-pipeline Error occurred while copying payload to $testResourcesDirectory - exception details ${e.printStackTrace()}")
        }
    }

    fun dropMessagesToABlobStorage() {
        val uniqueTimeStamp:String = getCurrentDateTimeWithSeconds().replace("\\s".toRegex(),"")
        val messagesMetadata = buildMetadata(uniqueTimeStamp)
        try {
            val blobServiceClient = BlobServiceClientBuilder()
                .connectionString(blobConnectionString)
                .buildClient()

            val containerClient: BlobContainerClient = blobServiceClient.getBlobContainerClient(blobContainerName)

            val directoryWithMessages = File(PATH_TO_MESSAGES)

            if (directoryWithMessages.exists()) {
                directoryWithMessages.listFiles()?.forEach {blobName->
                    val blobClient = containerClient.getBlobClient(blobName.name)
                    blobClient.upload(File(blobName.absolutePath).inputStream(), blobName.length(), true)
                    blobClient.setMetadata(messagesMetadata[blobName.name])
                    uploadedBlobs.add("$uniqueTimeStamp-${blobName.name}")
                }
            } else {
                throw FileNotFoundException("DEX::tst-hl7-pipeline HL7 messages not found under $PATH_TO_MESSAGES")
            }
        } catch (e:Exception) {
            logger.error("DEX::tst-hl7-pipeline Error occurred while uploading message to a blob storage: ${e.message}")
        }finally {
            println (uploadedBlobs)
            Thread.sleep(30_000)
            identifyCosmosDBContainerToQueryForEachBlob()

        }
    }
    private fun identifyCosmosDBContainerToQueryForEachBlob() {
        for (uploadedBlob in uploadedBlobs) {
            if (uploadedBlob.endsWith("PID5_ERROR.txt") || uploadedBlob.endsWith("NO_MSH3.txt")){
                queryCosmosDB(structureValidatorContainerName, uploadedBlob)
            }

            }
    }
    private fun queryCosmosDB(cosmosDBContainer:String, blobName:String) {
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

                //decide based on the filename which cosmos db container to query
                val queryCosmosDBToRetrievePayload =
                    "SELECT * FROM c WHERE c.metadata.provenance.ext_original_file_name=\"$blobName\""

                val payLoad = cosmosDBClient.sqlReadItems(queryCosmosDBToRetrievePayload, Map::class.java).blockLast()
                val jsonAsPayload = Gson().toJson(payLoad)
                addPayloadToTestResources(jsonAsPayload, blobName)
                Thread.sleep(5_000)
            }catch (e: Exception) {
            logger.error("DEX::tst-hl7-pipeline Error occurred while querying Cosmos DB: ${e.message}")

        }
    }
}
fun main() {
    val pipeline = PipelineTest()
    pipeline.dropMessagesToABlobStorage()
}