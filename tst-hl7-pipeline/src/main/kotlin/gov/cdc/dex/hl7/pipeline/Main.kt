package gov.cdc.dex.hl7.pipeline

import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.BlobContainerClient

import gov.cdc.dex.azure.cosmos.CosmosClient

import com.google.gson.Gson

import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileNotFoundException


class PipelineTest {
    companion object {
        var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
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
        val lakeOfSegmentsContainerName: String = System.getenv("LAKE_OF_SEGMENTS")

        val uploadedBlobs: MutableSet<String> = mutableSetOf()
        val utility = Utility()

    }
    fun dropMessagesToABlobStorage() {
        val uniqueTimeStamp:String = utility.getCurrentDateTimeWithSeconds().replace("\\s".toRegex(),"")
        val messagesMetadata =  utility.buildMetadata(uniqueTimeStamp)

        try {
            val blobServiceClient = BlobServiceClientBuilder()
                .connectionString(blobConnectionString)
                .buildClient()

            val containerClient: BlobContainerClient = blobServiceClient.getBlobContainerClient(blobContainerName)

            val directoryWithMessages = File(Constants.PATH_TO_MESSAGES)

            if (directoryWithMessages.exists()) {
                directoryWithMessages.listFiles()?.forEach {blobName->
                    val blobClient = containerClient.getBlobClient(blobName.name)
                    blobClient.upload(File(blobName.absolutePath).inputStream(), blobName.length(), true)
                    blobClient.setMetadata(messagesMetadata[blobName.name])
                    uploadedBlobs.add("$uniqueTimeStamp-${blobName.name}")
                }
            } else {
                throw FileNotFoundException("DEX::tst-hl7-pipeline HL7 messages not found under $Constants.PATH_TO_MESSAGES")
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
                uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH12) || uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_MSH21) ||
                uploadedBlob.endsWith(Constants.PHLIP_FLU_NO_PROFILE_IDENTIFIER) || uploadedBlob.endsWith(Constants.PHLIP_FLU_WITH_PID22) ||
                uploadedBlob.endsWith(Constants.PHLIP_FLU_DUPLICATE_OBX1) || uploadedBlob.endsWith(Constants.PHLIP_FLU_OBX2CWE_OBX5ST) ||
                uploadedBlob.endsWith(Constants.PHLIP_FLU_TWO_OBX_WITH_SAME_OBX3_DIFF_OBX4) || uploadedBlob.endsWith(Constants.PHLIP_FLU_TWO_OBX_WITH_SAME_OBX3_NULL_OBX4)){
                queryCosmosDB(structureValidatorContainerName, uploadedBlob)
            }else if (uploadedBlob.endsWith(Constants.PHLIP_FLU_VALID_MESSAGE) || uploadedBlob.endsWith(Constants.PHLIP_FLU_VALID_MESSAGE_WITH_PV1)){
                queryCosmosDB(jsonLakeContainerName, uploadedBlob)
            }else if (uploadedBlob.endsWith(Constants.PHLIP_FLU_TWO_OBX_WITH_SAME_OBX3_AND_OBX4)){
                queryCosmosDB(lakeOfSegmentsContainerName,uploadedBlob)
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

                val queryCosmosDBToRetrievePayload =
                    "SELECT * FROM c WHERE c.metadata.provenance.ext_original_file_name=\"$blobName\""

                val payLoad = cosmosDBClient.sqlReadItems(queryCosmosDBToRetrievePayload, Map::class.java).blockLast()
                val jsonAsPayload = Gson().toJson(payLoad)
                utility.addPayloadToTestResources(jsonAsPayload, blobName)
                Thread.sleep(5_000)
            }catch (e: Exception) {
            logger.error("DEX::tst-hl7-pipeline Error occurred while querying Cosmos DB: ${e.message}")

        }
    }
}