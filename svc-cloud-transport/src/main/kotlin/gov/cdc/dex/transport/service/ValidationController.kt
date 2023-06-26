package gov.cdc.dex.transport.service
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

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
import gov.cdc.dex.hl7.Helper
import cdc.xlr.structurevalidator._


/**
 *
 *
 * @Created - 6/23/23
 * @Author USY6@cdc.gov
 */
@Controller("/")
class ValidationController(private val cloudStorage: CloudStorage) {
    private val log = LoggerFactory.getLogger(TransportController::class.java.name)
    val gson: Gson = GsonBuilder().serializeNulls().create()

    @Post(value = "/default", consumes = [MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN])
    fun uploadContentDefault(
        @Body content: String,
        @QueryValue fileContentType: String = MediaType.TEXT_PLAIN
    ): HttpResponse<Any> {
        val resultData = this.validateMessage(content);


        return HttpResponse.ok(gson.toJson(resultData))
    }

    private fun validateMessage(hl7Content: String): String{
        var reportData = ""
        var redactorReport = ""
        val helper = Helper()
        var reportMsg = ""
        var reportList = ""
        var report = hl7Content?.let { helper.getRedactedReport(hl7Content,"CASE") }
        if (report != null) {
            reportMsg = "report msg :${report._1}"
            reportList = "report List: ${report._2()?.toList()}"
        }
        redactorReport = reportMsg + reportList

        reportData = redactorReport

        val validator = StructureValidatorAsync(ProfileLoaderLocal(PROFILES_PHIN_SPEC_3_1))   // the async validator 

        validator.reportMap( hl7Content ) match {
        
            case Success(report) => reportData = reportData + "report msg :${report._1}"
            case Failure(e) => reportData = reportData + "error: " + e.getMessage()
        
        }
        return reportData;
    }

    private fun getMetadata(request: HttpRequest<Any>): Map<String, String> {
        val headers = request.headers
        return headers
            .filter { it.key.startsWith("x-tp-") }
            .map { it.key.substring(5) to it.value.joinToString<String?>(";") }
            .toMap()
    }

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