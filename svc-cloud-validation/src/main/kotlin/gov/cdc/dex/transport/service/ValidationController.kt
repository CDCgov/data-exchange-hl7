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
//import gov.cdc.dex.hl7.Helper
//import cdc.xlr.structurevalidator._
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.OutputStream

import com.google.gson.*


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
    val redactorUrl = System.getenv("REDACTOR_URL") + "/api/redactorReport"
    val structureUrl = System.getenv("STRUCTURE_URL") + "/api/structure"
    val validationUrl = System.getenv("MMG_VALIDATOR_URL") + "/api/validate-mmg"
    val elrType = System.getenv("ELR_TYPE")

    @Post(value = "/validation", consumes = [MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN])
    fun uploadContentDefault(
        @Body content: String,
        @QueryValue fileContentType: String = MediaType.TEXT_PLAIN
    ): HttpResponse<Any> {
        val resultData = this.validateMessage(content);

        return HttpResponse.ok(gson.toJson(resultData))
    }

    private fun validateMessage(hl7Content: String): String{
        var reportData = ""

        var redactedMessage = this.getContent(URL(this.redactorUrl), hl7Content, "ELR")

        var structureReport = this.getContent(URL(this.structureUrl), redactedMessage, "CASE")

        var validationReport = this.getContent(URL(this.validationUrl), redactedMessage, "CASE")

        var jsonData = JsonObject()

        jsonData.add("StructureReport", JsonParser.parseString(gson.toJson(structureReport)))
        jsonData.add("ValidationReport", JsonParser.parseString(gson.toJson(validationReport)))
        reportData = jsonData.toString()

        return reportData
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
        conn.setRequestProperty("x-tp-route", elrType)
        var os: OutputStream
        try {
            val os = conn.getOutputStream()
            val input = payLoad.toByteArray()
            os.write(input, 0, input.size)
        }catch(e:Exception){
            throw Exception("Error reading output stream: ${e.message}")
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

}