package gov.cdc.dex.cloud.storage.azure

import com.azure.core.http.rest.PagedResponse
import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobClient
import com.azure.storage.blob.BlobServiceClient
import com.azure.storage.blob.BlobServiceClientBuilder
import com.azure.storage.blob.models.BlobItem
import com.azure.storage.blob.models.BlobProperties
import com.azure.storage.blob.models.ListBlobsOptions
import gov.cdc.dex.cloud.AzureConfig
import gov.cdc.dex.cloud.Providers
import gov.cdc.dex.cloud.storage.CloudFile
import gov.cdc.dex.cloud.storage.CloudStorage
import gov.cdc.dex.cloud.util.decode
import gov.cdc.dex.cloud.util.validateFor
import gov.cdc.dex.cloud.util.withMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*
import javax.inject.Singleton


private const val VAR_CONTAINER = "blob.container"

@Singleton
@Requires(property = "azure.blob")
class BlobProxy(private val azureConfig: AzureConfig, private val meterRegistry: MeterRegistry? = null) : CloudStorage {
    private val logger = LoggerFactory.getLogger(BlobProxy::class.java.name)

    init {
        logger.info("Initializing BlobProxy with config: {}", azureConfig)
    }

    override fun provider(): Providers = Providers.Azure

    private val blobServiceClient: BlobServiceClient = azureConfig.blob.connectStr.validateFor("blob.connectStr") {
        BlobServiceClientBuilder().connectionString(it).buildClient()
    }

    /**
     * This was introduced to be able to provide a 'silent' call to the aws s3 api
     */
    override fun healthCheck(): String = meterRegistry.withMetrics("blob.healthcheck") {
        TODO("Not yet implemented")
    }

    override fun getDefaultBucket(): String = azureConfig.blob.container ?: "N/A"

    override fun list(bucket: String, maxNumber: Int, prefix: String?) : List<String> =
        meterRegistry.withMetrics("blob.list") {
            listByType(bucket, maxNumber, prefix, BlobListType.FILE)
        }

    private fun listByType(bucket: String, maxNumber: Int, prefix: String?,
                           blobListType: BlobListType = BlobListType.ANY) : List<String> {
        val max = if (maxNumber > 0) maxNumber else 5000  // 5000 is Azure's default per page
        val blobContainerClient = blobServiceClient.getBlobContainerClient(bucket)
        val options = ListBlobsOptions().setMaxResultsPerPage(max)
        options.prefix = when {
            prefix != null -> prefix
            else -> "/"
        }
        // listing blobs returns all blobs, regardless of type.
        // we have set the max results per page, but no guarantee that they are all the type we want,
        // so we must filter each page and accumulate the results until we have the desired max
        // or there are no more results to be had.
        val pagedList : Iterable<PagedResponse<BlobItem>> =
            blobContainerClient.listBlobsByHierarchy("/", options, Duration.ofSeconds(30)).iterableByPage()
        val results = mutableListOf<String>()
        var iterations = 0
        val pageIterator = pagedList.iterator()
        while (results.size < max && iterations < max && pageIterator.hasNext()) {
            val page = pageIterator.next()
            val elements = if (blobListType != BlobListType.ANY) {
                page.elements.filter { it.isPrefix == (blobListType == BlobListType.FOLDER) }.map { b -> b.name }
            } else {
                page.elements.map { b -> b.name }
            }
            if (elements.isEmpty()) {
                iterations = max //break out of this loop
            } else {
                results.addAll(elements.take(max - results.size))
                iterations++
            }
        }
        return results
    }

    override fun list(maxNumber: Int, prefix: String?): List<String> =
        azureConfig.blob.container.validateFor(VAR_CONTAINER) { list(it, maxNumber, prefix) }

    override fun listFolders(bucket: String): List<String> = meterRegistry.withMetrics("blob.listFolders") {
        listByType(bucket, 10, null, BlobListType.FOLDER)
    }

    override fun listFolders(): List<String> =
        azureConfig.blob.container.validateFor(VAR_CONTAINER) { listFolders(it) }

    override fun getFileContent(bucket: String, fileName: String): String =
        meterRegistry.withMetrics("blob.getFileContent") {
            val blobContainerClient = blobServiceClient.getBlobContainerClient(bucket)
            val blobClient = blobContainerClient.getBlobClient(fileName)
            val input = blobClient.openInputStream() as InputStream
            val inputStream = InputStreamReader(input, StandardCharsets.UTF_8)
            val s = Scanner(inputStream).useDelimiter("\\A")
            if (s.hasNext()) s.next() else ""
        }

    override fun getFile(fileName: String): CloudFile = azureConfig.blob.container.validateFor(VAR_CONTAINER) {
        getFile(it, fileName)
    }

    override fun getFileContent(fileName: String): String =
        azureConfig.blob.container.validateFor(VAR_CONTAINER) { getFileContent(it, fileName) }

    override fun getFileContentAsInputStream(bucket: String, fileName: String): InputStream =
        meterRegistry.withMetrics("blob.getFileContentAsInputStream") {
            TODO("Not yet implemented")
        }

    override fun getFileContentAsInputStream(fileName: String): InputStream =
        azureConfig.blob.container.validateFor(VAR_CONTAINER) { getFileContentAsInputStream(it, fileName) }

    override fun getMetadata(bucket: String, fileName: String, urlDecode: Boolean): Map<String, String> =
        meterRegistry.withMetrics("blob.getMetadata") {
            runCatching {
                getProperties(bucket, fileName).let {
                    mapOf("last_modified" to it.lastModified.toString()).plus(
                        if (urlDecode) it.metadata.decode() else it.metadata
                    )
                }
            }.onFailure {
                logger.error(
                    "Failed to get MetaData for fileName: {} in bucket: {}. Exception: {}", fileName, bucket, "${it.message}"
                )
            }.getOrThrow()
        }

    override fun getMetadata(fileName: String, urlDecode: Boolean): Map<String, String> =
        azureConfig.blob.container.validateFor(VAR_CONTAINER) { getMetadata(it, fileName, urlDecode) }

    override fun saveFile(bucket: String, fileName: String, content: String, metadata: Map<String, String>?, contentType: String
    ) = meterRegistry.withMetrics("blob.saveFile") {
        val binaryData = BinaryData.fromString(content)
        val blobContainerClient = blobServiceClient.getBlobContainerClient(bucket)
        val blobClient: BlobClient = blobContainerClient.getBlobClient(fileName)
        blobClient.upload(binaryData, true)
        if (metadata != null) {
            blobClient.setMetadata(metadata)
        }
    }

    override fun saveFile(bucket: String, fileName: String, content: InputStream, size: Long, metadata: Map<String, String>?, contentType: String
    ) = meterRegistry.withMetrics("blob.saveFile.inputStream") {
        val binaryData = BinaryData.fromStream(content)
        val blobContainerClient = blobServiceClient.getBlobContainerClient(bucket)
        val blobClient: BlobClient = blobContainerClient.getBlobClient(fileName)
        blobClient.upload(binaryData, true)
        if (metadata != null) {
            blobClient.setMetadata(metadata)
        }
    }

    override fun saveFile(fileName: String, content: String, metadata: Map<String, String>?, contentType: String) =
        azureConfig.blob.container.validateFor(VAR_CONTAINER) {saveFile(it, fileName, content, metadata, contentType)}


    override fun saveFile(
        fileName: String,
        content: InputStream,
        size: Long,
        metadata: Map<String, String>?,
        contentType: String
    ) = azureConfig.blob.container.validateFor(VAR_CONTAINER) {saveFile(it, fileName, content, size, metadata, contentType)}

    override fun deleteFile(bucket: String, fileName: String): Int = meterRegistry.withMetrics("blob.deleteFile") {
        TODO("Not yet implemented")
    }

    override fun deleteFile(fileName: String): Int =
        azureConfig.blob.container.validateFor(VAR_CONTAINER) { deleteFile(it, fileName) }

    private fun getProperties(container: String, key: String): BlobProperties {
        val blobContainerClient = blobServiceClient.getBlobContainerClient(container)
        val blobClient = blobContainerClient.getBlobClient(key)
        return blobClient.properties
    }

}
