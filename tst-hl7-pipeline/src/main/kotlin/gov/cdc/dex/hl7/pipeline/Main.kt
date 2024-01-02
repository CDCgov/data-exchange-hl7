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
        val cosmosDBEndpoint: String = System.getenv("COSMOS_DB_ENDPOINT")
        val cosmosDBPartitionKey: String = System.getenv("COSMOS_DB_PARTITION_KEY")
        val blobConnectionString: String = System.getenv("BLOB_INGEST_CONNECTION_STRING")
        val blobContainerName: String = System.getenv("BLOB_CONTAINER_NAME")
        val receiverDebatcherContainerName: String = System.getenv("RECEIVER_DEBATCHER")
        val redactorContainerName: String = System.getenv("REDACTOR")
        val structureValidatorContainerName: String = System.getenv("STRUCTURE_VALIDATOR")
        val jsonLakeContainerName: String = System.getenv("JSON_LAKE")
        val lakeOfSegmentsContainerName: String = System.getenv("LAKE_OF_SEGMENTS")

        const val PATH_TO_MESSAGES = "src/main/resources/messages"
        private const val MESSAGE_TYPE = "message_type"
        private const val ROUTE = "route"
        private const val REPORTING_JURISDICTION:String = "reporting_jurisdiction"
        private const val ORIGINAL_FILE_NAME:String = "original_file_name"

        val uploadedBlobs: MutableSet<String> = mutableSetOf()

    }

    private fun buildMetadata(uniqueTimeStamp: String): Map<String, MutableMap<String, out String?>> {
        return mapOf(
            "COVID19_Missing_MSH3.txt" to mutableMapOf<String, String?>(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID19_Missing_MSH3.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID19_Missing_MSH4.txt" to mutableMapOf<String, String?>(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID19_Missing_MSH4.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID19_Missing_MSH9.txt" to mutableMapOf<String, String?>(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID19_Missing_MSH9.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID19_Missing_MSH12.txt" to mutableMapOf<String, String?>(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID19_Missing_MSH12.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID19_PID_Segment_With_Patient_Name_And_Address.txt" to mutableMapOf(
                MESSAGE_TYPE to "ELR",
                REPORTING_JURISDICTION to "48",
                ROUTE to "COVID19_ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID19_PID_Segment_With_Patient_Name_And_Address.txt"
            ),
            "COVID-19_OBX1_Uniqueness_Test.txt" to mutableMapOf(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID-19_OBX1_Uniqueness_Test.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID-19_OBX2_CWE_OBX5_NM.txt" to mutableMapOf(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID-19_OBX2_CWE_OBX5_NM.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID-19_OBX2CWE_OBX5_CWE.txt" to mutableMapOf(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to uniqueTimeStamp,
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID-19_OBX_Sequential_Test.txt" to mutableMapOf(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID-19_OBX2CWE_OBX5_CWE.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID-19_PID_DateOfBirth.txt" to mutableMapOf(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID-19_PID_DateOfBirth.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID-19_PID_Required_Fields.txt" to mutableMapOf(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID-19_PID_Required_Fields.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID-19_With_SSN_PID19.txt" to mutableMapOf(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID-19_With_SSN_PID19.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "COVID-19_With_SSN_PID19.txt" to mutableMapOf(
                MESSAGE_TYPE to "ELR",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-COVID-19_With_SSN_PID19.txt",
                ROUTE to "COVID19_ELR",
                REPORTING_JURISDICTION to "48"
            ),
            "FDD_LIST_PID_Name_Address_CASE.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-FDD_LIST_PID_Name_Address_CASE.txt",
                ROUTE to "",
                REPORTING_JURISDICTION to "48"
            ),
            "PHLIP_DataType_DT_CASE.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_DataType_DT_CASE.txt",
                ROUTE to "",
                REPORTING_JURISDICTION to "48"
            ),

            "PHLIP_FLU_DataType_CWE.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_FLU_DataType_CWE.txt"

            ),
            "PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt"
            ),
            "PHLIP_FLU_PID_7_DateTimeOfBirth.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_FLU_PID_7_DateTimeOfBirth.txt"
            ),
            "PHLIP_FLU_PID_19.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_FLU_PID_19.txt"
            ),
            "PHLIP_FLU_Receiving_Sending_Applications.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_FLU_Receiving_Sending_Applications.txt"
            ),
            "PHLIP_OBX2_CWE_OBX5_ST.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_OBX2_CWE_OBX5_ST.txt"
            ),
            "PHLIP_Salm_PID_Required_Fields_Case.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_Salm_PID_Required_Fields_Case.txt"
            ),
            "PHLIP_VPD_DataType_ED.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_VPD_DataType_ED.txt"
            ),
            "PHLIP_VPD_VALID.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_VPD_VALID.txt"
            ),
            "PHLIP_VPD_VALID_DataType_FT.txt" to mutableMapOf(
                MESSAGE_TYPE to "CASE",
                REPORTING_JURISDICTION to "48",
                ORIGINAL_FILE_NAME to "$uniqueTimeStamp-PHLIP_VPD_VALID_DataType_FT.txt"
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
    private fun addPayloadToTestResources(payloadAsJson: Map<*, *>?, originalFileName: String) {
        val testResourcesDirectory = "src/test/resources/new-payloads"
        try {
            val jsonFileWithPayload = File("$testResourcesDirectory/${originalFileName.replace(".txt","")}.json")
            jsonFileWithPayload.writeText(payloadAsJson.toString())

        }catch (e:Exception) {
            logger.error("DEX::tst-hl7-pipeline Error occurred while copying payload to $testResourcesDirectory")
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
                directoryWithMessages.listFiles()?.forEach {
                    val blobNameWithTimeAppended = uniqueTimeStamp + it.name
                    val blobClient = containerClient.getBlobClient(blobNameWithTimeAppended)
                    blobClient.upload(File(it.absolutePath).inputStream(), it.length(), true)
                    blobClient.setMetadata(messagesMetadata[it.name])
                    uploadedBlobs.add("$uniqueTimeStamp-${it.name}")
                }
            } else {
                throw FileNotFoundException("DEX::tst-hl7-pipeline HL7 messages not found under $PATH_TO_MESSAGES")
            }
        } catch (e:Exception) {
            logger.error("DEX::tst-hl7-pipeline Error occurred while uploading message to a blob storage: ${e.message}")
        }finally {
            Thread.sleep(30_000)
            identifyCosmosDBContainerToQueryForEachBlob()

        }
    }
    private fun identifyCosmosDBContainerToQueryForEachBlob() {
        for (uploadedBlob in uploadedBlobs) {
            if (uploadedBlob.endsWith("Missing_MSH3.txt") || uploadedBlob.endsWith("Missing_MSH4.txt") || uploadedBlob.endsWith(
                    "Missing_MSH9.txt") || uploadedBlob.endsWith("Missing_MSH12.txt")) {
                queryCosmosDB(receiverDebatcherContainerName, uploadedBlob)
            } else if (uploadedBlob.endsWith("With_Patient_Name_And_Address.txt") || uploadedBlob.endsWith("SSN_PID19.txt") || uploadedBlob.endsWith(
                    "PID_Name_Address_CASE.txt") || uploadedBlob.endsWith("FLU_PID_19.txt")){
                queryCosmosDB(redactorContainerName, uploadedBlob)
            }else if (uploadedBlob.endsWith("Uniqueness_Test.txt") || uploadedBlob.endsWith("CWE_OBX5_NM.txt") || uploadedBlob.endsWith("Sequential_Test.txt") || uploadedBlob.endsWith("OBX2_CWE_OBX5_ST.txt")){
                queryCosmosDB(structureValidatorContainerName,uploadedBlob)

            }else {
                queryCosmosDB(jsonLakeContainerName,uploadedBlob)
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

                val queryItem = cosmosDBClient.sqlReadItems(queryCosmosDBToRetrievePayload, Map::class.java).blockLast()
                addPayloadToTestResources(queryItem, blobName)
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