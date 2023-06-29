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
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.OutputStream

/**
 *
 *
 * @Created - 6/23/23
 * @Author USY6@cdc.gov
 */
@Controller("/")
class ValidationController(private val cloudStorage: CloudStorage) {
    private val log = LoggerFactory.getLogger(ValidationController::class.java.name)
    val gson: Gson = GsonBuilder().serializeNulls().create()
    val redactorUrl = "{{REDACTOR_URL}}/api/redactorReport"
    val structureUrl = "{{STRUCTURE_URL}}/api/structure"
    val validationUrl = "{{MMG_VALIDATOR_URL}}/api/validate-mmg"


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

        var redactorReport = this.getContent(URL(this.redactorUrl), hl7Content, "ELR")

        var structureReport = this.getContent(URL(this.structureUrl), hl7Content, "CASE")

        var validationReport = this.getContent(URL(this.validationUrl), hl7Content, "CASE")

        reportData = redactorReport + structureReport + validationReport

        return reportData;
    }

    private fun getMetadata(request: HttpRequest<Any>): Map<String, String> {
        val headers = request.headers
        return headers
            .filter { it.key.startsWith("x-tp-") }
            .map { it.key.substring(5) to it.value.joinToString<String?>(";") }
            .toMap()
    }

    private fun getContent(url: URL, payLoad : String, msgType: String): String {
        val sb = StringBuilder()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("x-tp-message_type", msgType)
        conn.setRequestProperty("x-tp-route", "COVID19-ELR")
        var os: OutputStream
        try(os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        var br : BufferedReader? = null
        try {
            if (conn.responseCode != 200) {
                throw RuntimeException("Failed : HTTP error code : " + conn.responseCode)
            }
            br = BufferedReader(InputStreamReader((conn.inputStream)))
            var line: String?
            while ((br.readLine().also { line = it }) != null) {
                sb.append(line)
            }
        }catch(e:Exception){
            throw Exception("Error reading input stream: ${e.message}")
        }
        finally{
            br?.close()
        }
        return sb.toString()
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