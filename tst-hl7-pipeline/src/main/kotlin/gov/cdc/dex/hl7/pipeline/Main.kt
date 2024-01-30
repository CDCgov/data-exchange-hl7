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
        val jsonLakeContainerName: String = System.getenv("JSON_LAKE")
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
            Constants.PHLIP_FLU_NO_MSH3 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to  Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH3}",
                ROUTE to Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH4 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH4}",
                ROUTE to Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH5 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH5}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH6 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH6}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH7 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH7}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH9 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH9}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH10 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH10}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH11 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH11}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH12 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH12}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH21 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH21}",
                ROUTE to Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_PROFILE_IDENTIFIER to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_PROFILE_IDENTIFIER}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_VALID_MESSAGE to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_VALID_MESSAGE}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_VALID_MESSAGE_WITH_PV1 to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_VALID_MESSAGE_WITH_PV1}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_PID5_ERROR to mutableMapOf<String, String?>(
                MESSAGE_TYPE to Constants.MESSAGE_TYPE_ELR,
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_PID5_ERROR}",
                ROUTE to  Constants.PHLIP_FLU,
                REPORTING_JURISDICTION to Constants.JURISDICTION
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
            if (uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH3) || uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH4) ||
                uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH5) || uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH6) ||
                uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH7) || uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH9) ||
                uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH10) || uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH11) ||
                uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH12) || uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH21)||
                uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_PROFILE_IDENTIFIER)){
                queryCosmosDB(structureValidatorContainerName, uploadedBlob)
            }else if (uploadedBlob.endsWith(Constants.PHLIP_FLU_VALID_MESSAGE) || uploadedBlob.endsWith(Constants.PHLIP_FLU_VALID_MESSAGE_WITH_PV1)){
                queryCosmosDB(jsonLakeContainerName, uploadedBlob)
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