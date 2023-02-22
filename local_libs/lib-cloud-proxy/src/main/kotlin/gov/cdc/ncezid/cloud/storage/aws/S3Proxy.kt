package gov.cdc.ncezid.cloud.storage.aws

import gov.cdc.ncezid.cloud.AWSConfig
import gov.cdc.ncezid.cloud.Providers
import gov.cdc.ncezid.cloud.storage.CloudFile
import gov.cdc.ncezid.cloud.storage.CloudStorage
import gov.cdc.ncezid.cloud.storage.META_CONTENT_ENCODING
import gov.cdc.ncezid.cloud.storage.META_CONTENT_LENGTH
import gov.cdc.ncezid.cloud.storage.META_CONTENT_TYPE
import gov.cdc.ncezid.cloud.util.decode
import gov.cdc.ncezid.cloud.util.validateFor
import gov.cdc.ncezid.cloud.util.withMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.annotation.Requires
import io.micronaut.http.MediaType
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CompletedPart
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.Duration
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Singleton

private const val FILE_5MB = (5 * 1024 * 1024).toLong()
private const val VAR_BUCKET = "s3.bucket"

@Singleton
@Requires(property = "aws.s3")
class S3Proxy(private val awsConfig: AWSConfig, private val meterRegistry: MeterRegistry? = null) : CloudStorage {
    private val logger = LoggerFactory.getLogger(S3Proxy::class.java.name)

    init {
        logger.info("AUDIT- Initializing AWS S3Proxy with config: {}", awsConfig)
    }

    override fun provider(): Providers = Providers.AWS

    private val s3Client = S3Client.builder().overrideConfiguration {
        it.apiCallTimeout(Duration.ofSeconds(awsConfig.s3.apiCallTimeoutSeconds))
            .apiCallAttemptTimeout(Duration.ofSeconds(awsConfig.s3.apiCallAttemptTimeoutSeconds))
    }.region(Region.of(awsConfig.region)).build()

    override fun getFile(fileName: String): CloudFile = awsConfig.s3.bucket.validateFor(VAR_BUCKET) {
        getFile(it, fileName)
    }

    override fun getDefaultBucket(): String = awsConfig.s3.bucket ?: "N/A"

    @Throws
    override fun getFileContent(bucket: String, fileName: String): String =
        meterRegistry.withMetrics("s3.getFileContent") {
            logger.debug("Getting fileContent for fileName: {} in bucket: {}", fileName, bucket)
            runCatching {
                try {
                    s3Client.getObject { it.bucket(bucket).key(fileName) }.bufferedReader().use { it.readText() }
                } catch (e: NoSuchKeyException) {
                    getZipContent(bucket, fileName)
                }
            }.onFailure {
                logger.error(
                    "Failed to get fileContent for fileName: {} in bucket: {}. Exception: {}",
                    fileName,
                    bucket,
                    it.toString()
                )
            }.getOrThrow()
        }

    // TODO - should throw exception if bucket doesn't exist (and same for others)
    override fun getFileContent(fileName: String): String =
        awsConfig.s3.bucket.validateFor(VAR_BUCKET) { getFileContent(it, fileName) }

    override fun getFileContentAsInputStream(bucket: String, fileName: String): InputStream =
        meterRegistry.withMetrics("s3.getFileContentAsInputStream") {
            logger.debug("Getting fileContent for fileName: {} in bucket: {}", fileName, bucket)

            runCatching {
                ByteArrayInputStream(s3Client.getObject { it.bucket(bucket).key(fileName) }.readAllBytes())
            }.onFailure {
                logger.error(
                    "Failed to get fileContent (as InputStream) for fileName: {} in bucket: {}. Exception: {}",
                    fileName,
                    bucket,
                    it.toString()
                )
            }.getOrThrow()
        }

    override fun getFileContentAsInputStream(fileName: String): InputStream =
        awsConfig.s3.bucket.validateFor(VAR_BUCKET) { getFileContentAsInputStream(it, fileName) }

    @Throws
    override fun getMetadata(bucket: String, fileName: String, urlDecode: Boolean): Map<String, String> =
        meterRegistry.withMetrics("s3.getMetadata") {
            logger.debug("Getting metaData for fileName: {} in bucket: {}", fileName, bucket)

            runCatching {
                s3Client.headObject { it.bucket(bucket).key(fileName) }.let {
                    mapOf("last_modified" to it.lastModified().toString())
                        .plus(if (urlDecode) it.metadata().decode() else it.metadata())
                        .plus(META_CONTENT_LENGTH to it.contentLength().toString())
                        .plus(META_CONTENT_TYPE to it.contentType())
                        .plus(META_CONTENT_ENCODING to it.contentEncoding())
                }
            }.onSuccess {
                logger.debug("S3 File Metadata: {}", it)
            }.onFailure {
                logger.error(
                    "Failed to get MetaData for fileName: {} in bucket: {}. Exception: {}", fileName, bucket, it.toString()
                )
            }.getOrThrow()
        }


    override fun getMetadata(fileName: String, urlDecode: Boolean): Map<String, String> =
        awsConfig.s3.bucket.validateFor(VAR_BUCKET) { getMetadata(it, fileName, urlDecode) }

    override fun saveFile(
        bucket: String,
        fileName: String,
        content: String,
        metadata: Map<String, String>?,
        contentType: String
    ): Unit = meterRegistry.withMetrics("s3.saveFile") {
        logger.debug(
            "Saving File: {} to bucket: {}, with contentType: {} and MetaData: {} ",
            fileName, bucket, contentType, metadata
        )

        runCatching {
            s3Client.putObject({
                it.key(fileName)
                    .contentType(contentType)
                    .bucket(bucket)
                    .metadata(metadata)
            }, RequestBody.fromString(content)).also {
                logger.info("\tfile stored to S3 response: $it")
            }
        }.onFailure {
            logger.error("Failed to Save File for fileName: {} in bucket: {}. Exception: {}", fileName, bucket, it.toString())
        }.getOrThrow()
    }

    override fun saveFile(
        bucket: String,
        fileName: String,
        content: InputStream,
        size: Long,
        metadata: Map<String, String>?,
        contentType: String
    ): Unit = meterRegistry.withMetrics("s3.saveFile.inputstream") {
        runCatching {
            if (size > FILE_5MB)
                uploadMultipartFile(bucket, content, fileName, size, metadata ?: mapOf())
            else uploadSinglePart(bucket, fileName, content, size, contentType, metadata)
        }.onFailure {
            logger.error(
                "Failed to Save File (from InputStream) for fileName: {} in bucket: {}. Exception: {}",
                fileName, bucket, it.toString()
            )
        }.getOrThrow()
    }

    override fun saveFile(fileName: String, content: String, metadata: Map<String, String>?, contentType: String) =
        awsConfig.s3.bucket.validateFor(VAR_BUCKET) { saveFile(it, fileName, content, metadata, contentType) }


    override fun saveFile(
        fileName: String,
        content: InputStream,
        size: Long,
        metadata: Map<String, String>?,
        contentType: String
    ): Unit =
        awsConfig.s3.bucket.validateFor(VAR_BUCKET) { saveFile(it, fileName, content, size, metadata, contentType) }

    //Retrieves the first X number of files on a folder (if prefix is provided) or root if prefix is null:
    override fun list(bucket: String, maxNumber: Int, prefix: String?): List<String> =
        meterRegistry.withMetrics("s3.list") {
            logger.debug("Listing max ({}) objects for bucket: {} with prefix {}", maxNumber, bucket, prefix)

            runCatching {
                s3Client.listObjects { l -> l.bucket(bucket).maxKeys(maxNumber).prefix(prefix) }.let { res ->
                    logger.debug("Response has contents: {}", res.hasContents())
                    res.contents().map { it.key() }
                }
            }.onFailure {
                logger.error("Failed to List objects for bucket: {} with prefix {}. Exception: {}", bucket, prefix, it.toString())
            }.getOrThrow()
        }

    /**
     * This was introduced to be able to provide a 'silent' call to the aws s3 api
     */
    override fun healthCheck(): String = meterRegistry.withMetrics("s3.healthcheck") {
        awsConfig.s3.bucket.validateFor(VAR_BUCKET) {
            s3Client.listObjects { req -> req.bucket(it).maxKeys(1) }.responseMetadata().requestId()
        }
    }

    override fun list(maxNumber: Int, prefix: String?): List<String> =
        awsConfig.s3.bucket.validateFor(VAR_BUCKET) { list(it, maxNumber, prefix) }

    override fun listFolders(bucket: String): List<String> = meterRegistry.withMetrics("s3.listFolders") {
        runCatching {
            with(list(bucket, 100)) {
                logger.debug("List: {}", this)
                map { it.split("/").first() }.distinctBy { it }
            }
        }.onFailure {
            logger.error("Failed to List Folders for bucket: {}. Exception: {}", bucket, it.toString())
        }.getOrThrow()
    }

    override fun listFolders(): List<String> = awsConfig.s3.bucket.validateFor(VAR_BUCKET) { listFolders(it) }

    override fun deleteFile(bucket: String, fileName: String): Int =
        meterRegistry.withMetrics("s3.deleteFile") {
            runCatching {
                val keyAsList = listOf(ObjectIdentifier.builder().key(fileName).build())

                s3Client.deleteObjects { req ->
                    req.bucket(bucket).delete { it.objects(keyAsList) }
                }.deleted().size
            }.onFailure {
                logger.error("Failed to Delete file: {} in bucket: {}. Exception: {}", fileName, bucket, it.toString())
            }.getOrThrow()
        }

    override fun deleteFile(fileName: String): Int =
        awsConfig.s3.bucket.validateFor(VAR_BUCKET) { deleteFile(it, fileName) }

    private fun uploadSinglePart(
        bucket: String,
        fileName: String,
        content: InputStream,
        contentLength: Long,
        contentType: String = MediaType.TEXT_PLAIN,
        metadata: Map<String, String>? = null
    ) {
        logger.debug(
            "Uploading singlepart " +
            "fileName: $fileName, " +
            "bucket: $bucket, " +
            "contentLength: $contentLength, " +
            "contentType: $contentType"
        )

        s3Client.putObject({ pob ->
            pob.key(fileName)
                .contentType(contentType)
                .bucket(bucket)
                .metadata(metadata)
        }, RequestBody.fromInputStream(content, contentLength))
    }

    //Multipart uploads:
    private fun uploadMultipartFile(
        bucket: String,
        file: InputStream,
        filename: String,
        contentLength: Long,
        metadata: Map<String, String>
    ) {
        logger.debug("Processing multipart bucket: $bucket, key: $filename.")
        //Create multipart upload request and response
        val uploadId: String = createMultipartUpload(filename, bucket, metadata)
        //upload part by part
        val completedPartList = uploadMultiParts(file, contentLength, bucket, filename, uploadId)
        // merge all parts uploaded
        completeMultiParts(completedPartList, bucket, filename, uploadId).also {
            logger.debug("Uploaded Multipart file. Received eTag: {}", it)
        }
    }

    private fun createMultipartUpload(key: String, bucket: String, metadata: Map<String, String>): String {
        logger.debug("Creating MultipartUpload with key: {}, bucket: {}, metaData: {}", key, bucket, metadata)
        return s3Client.createMultipartUpload { mur -> mur.key(key).bucket(bucket).metadata(metadata) }.uploadId()
    }

    @Throws(IOException::class)
    private fun uploadMultiParts(
        file: InputStream,
        contentLength: Long,
        bucket: String,
        key: String,
        uploadId: String,
    ): List<CompletedPart> {
        var partSize = FILE_5MB // Set part size to 5 MB.
        var filePosition: Long = 0
        val completedPartList: MutableList<CompletedPart> = ArrayList()
        var i = 1
        while (filePosition < contentLength) {
            // last part could be less than 5 MB, adjust the part size as needed.
            partSize = partSize.coerceAtMost(contentLength - filePosition)
            //s3Client.upload
            val eTag = s3Client.uploadPart({ upb ->
                upb.bucket(bucket).key(key).uploadId(uploadId).partNumber(i).contentLength(partSize)
            }, RequestBody.fromInputStream(file, contentLength)).eTag()

            logger.debug("part# $i , eTag: $eTag , fileposition: ${filePosition / (1024 * 1024)}MB, partsize: ${partSize / (1024 * 1024)}MB, contentLength: $contentLength.")
            val completedPart = CompletedPart.builder().partNumber(i).eTag(eTag).build()
            completedPartList.add(completedPart)
            filePosition += partSize
            i++
        }
        return completedPartList
    }

    private fun completeMultiParts(
        completedPartList: List<CompletedPart>,
        bucket: String,
        key: String,
        uploadId: String,
    ): String {
        logger.debug("Completing MultipartUpload with key: {}, bucket: {}, uploadId: {}", key, bucket, uploadId)
        return s3Client.completeMultipartUpload { cmurb ->
            cmurb.bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload { cmub -> cmub.parts(completedPartList) }
        }.eTag().replace("\"".toRegex(), "")
    }

    private fun getZipContent(bucket: String, config: String): String {
        logger.debug("Getting Zip Content for bucket: {}", bucket)
        val req = GetObjectRequest.builder().bucket(bucket).key("${config}.zip").build()
        val res = s3Client.getObject(req)
        val s3Content = res.readAllBytes()

        val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".tmp")
        tempFile.writeBytes(s3Content)
        try {
            ZipFile(tempFile).use { zf ->
                logger.debug(String.format("Inspecting contents of: %s\n", zf.name))
                val zipEntries = zf.entries()
                val iterator: Iterator<*> = zipEntries.asIterator() //.forEachRemaining(entry -> {
                //while (iterator.hasNext()) {
                val entry = iterator.next() as ZipEntry
                return zf.getInputStream(entry).bufferedReader().use(BufferedReader::readText)
                //}
            }
        } finally {
            tempFile.delete()
        }
    }
}
