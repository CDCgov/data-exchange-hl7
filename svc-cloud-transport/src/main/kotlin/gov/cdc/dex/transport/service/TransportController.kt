package gov.cdc.dex.transport.service

import gov.cdc.dex.cloud.storage.CloudStorage
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.multipart.CompletedFileUpload
import org.slf4j.LoggerFactory
import java.util.*

/**
 *
 *
 * @Created - 5/12/20
 * @Author Marcelo Caldas mcq1@cdc.gov
 */
@Controller("/")
class TransportController(private val cloudStorage: CloudStorage) {
    private val log = LoggerFactory.getLogger(TransportController::class.java.name)

    @Post(value = "/encode", consumes = [MediaType.TEXT_PLAIN])
    fun encode(@Body body: String?): String? {
        log.info("AUDIT:: encoding message")
        return Base64.getEncoder().encodeToString(body!!.toByteArray())
    }

    @Post(value = "/default", consumes = [MediaType.MULTIPART_FORM_DATA])
    fun uploadFileDefault(
        @Body file: CompletedFileUpload,
        @QueryValue fileContentType: String?,
        request: HttpRequest<Any>
    ) {
        cloudStorage.saveFile(
            file.filename,
            file.inputStream,
            file.size,
            getMetadata(request),
            fileContentType ?: MediaType.TEXT_PLAIN,
        )
    }

    @Post(value = "/{bucket}", consumes = [MediaType.MULTIPART_FORM_DATA])
    fun uploadFile(
        @Body file: CompletedFileUpload,
        @PathVariable bucket: String,
        @QueryValue fileContentType: String = MediaType.TEXT_PLAIN,
        request: HttpRequest<Any>
    ) {
        cloudStorage.saveFile(
            bucket,
            file.filename,
            file.inputStream,
            file.size,
            getMetadata(request),
            fileContentType,
        )
    }

    @Post(value = "/default", consumes = [MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN])
    fun uploadContentDefault(
        @Body content: String,
        @QueryValue filename: String?,
        @QueryValue fileContentType: String = MediaType.TEXT_PLAIN,
        request: HttpRequest<Any>
    ): HttpResponse<Any> {
        val guid = filename ?: UUID.randomUUID().toString()
        cloudStorage.saveFile(guid, content, getMetadata(request), fileContentType)
        log.info("Accepted message $guid")
        return HttpResponse.ok(guid)
    }

    @Post(value = "/{bucket}", consumes = [MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN])
    fun uploadContent(
        @Body content: String,
        @PathVariable bucket: String,
        @QueryValue filename: String?,
        @QueryValue fileContentType: String?,
        request: HttpRequest<Any>
    ): HttpResponse<Any> {
        val guid = filename ?: UUID.randomUUID().toString()
        cloudStorage.saveFile(bucket, guid, content, getMetadata(request), fileContentType ?: "text/plain")
        log.info("Accepted message $guid")
        return HttpResponse.ok(guid)
    }

    private fun getMetadata(request: HttpRequest<Any>): Map<String, String> {
        val headers = request.headers
        return headers
            .filter { it.key.startsWith("x-tp-") }
            .associate {
                it.key.substring(5) to (it.value.firstOrNull() ?: "") }
    }

    @Get(value = "/heartbeat")
    fun getHeartbeatPingResponse() : String {
        return "hello"
    }

    @Get(value = "/{bucket}")
    fun getListOfFiles(@PathVariable bucket: String, @QueryValue prefix: String? = null): List<String> {
        log.info("AUDIT - Getting List of files")
        return cloudStorage.list(bucket, 100, prefix)
    }

    @Get(value = "/default")
    fun getListOfFilesDefault(@QueryValue prefix: String? = null): List<String> {
        return cloudStorage.list(100, prefix)
    }

    //TODO::Key does not allow prefixes, like folder/filename - Needs improvement
    @Get(value = "/{bucket:[a-zA-Z0-9-_\\.\\/]+}/{key}")
    fun getContent(@PathVariable bucket: String, @PathVariable key: String): String  {
        log.info("AUDIT - Getting file content ${bucket} -> $key")
        return cloudStorage.getFileContent(bucket, key)
    }

    @Get(value = "/default/{key:[a-zA-Z0-9-_\\.\\/]+}")
    fun getContentDefault(@PathVariable key: String) = cloudStorage.getFileContent(key)

    @Get(value = "/{bucket}/{key}/metadata")
    fun getMetadata(@PathVariable bucket: String, @PathVariable key: String) = cloudStorage.getMetadata(bucket, key)

    @Get(value = "/default/{key}/metadata")
    fun getMetadataDefault(@PathVariable key: String) = cloudStorage.getMetadata(key)

    @Delete(value = "/{bucket}/{key}")
    fun deleteObject(@PathVariable bucket: String, @PathVariable key: String) = cloudStorage.deleteFile(bucket, key)

    @Delete(value = "/default/{key}")
    fun deleteObjectDefault(@PathVariable key: String) = cloudStorage.deleteFile(key)


}