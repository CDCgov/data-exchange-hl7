package gov.cdc.dex.validation.service

//import gov.cdc.dex.hl7.Helper
//import cdc.xlr.structurevalidator._

import com.google.gson.*
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory
import java.io.*
import java.net.*
import java.net.http.HttpClient
import java.nio.charset.StandardCharsets.*
import java.util.*


/**
 *
 *
 * @Created - 6/23/23
 * @Author USY6@cdc.gov
 */
@Controller("/validation")
class ValidationController() {
    private val log = LoggerFactory.getLogger(ValidationController::class.java.name)
    val gson: Gson = GsonBuilder().serializeNulls().create()

    val redactorUrl = System.getenv("REDACTOR_URL") + "/api/redactorReport"
    val structureUrl = System.getenv("STRUCTURE_URL") + "/api/structure"

    @Post(value = "/", consumes = [MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN])
    fun uploadContentDefault(
        @Body content: String,
        request: HttpRequest<Any>
    ): HttpResponse<Any> {
        val resultData = this.validateMessage(request, content);

        return HttpResponse.ok(gson.toJson(resultData))
    }

    private fun validateMessage(request: HttpRequest<Any>, hl7Content: String): String{
        var reportData = ""

        var redactedMessage = this.getContent(URL(this.redactorUrl), hl7Content, getMetadata(request))

        var structureReport = this.getContent(URL(this.structureUrl), redactedMessage, getMetadata(request))

        var jsonData = JsonObject()

        jsonData.add("StructureReport", JsonParser.parseString(gson.toJson(structureReport)))
        reportData = jsonData.toString()

        return reportData
    }

    private fun getContent(url: URL, payLoad : String, metadata: Map<String, String>? = null): String {
        val client = HttpClient.newBuilder().build()

        val urlPath = url.toString()
        val messageType = metadata?.get("message_type")

        val request = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create(urlPath))
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payLoad))
            .setHeader("x-tp-message_type", messageType)
            .build()

        val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).body()

        var finalValue = response.replace("{\"_1\":", "")
        finalValue = finalValue.replace(",\"_2\":[]}", "")
        finalValue = finalValue.replace("\\\\u0026", "&")
        finalValue = finalValue.replace("\"", "")

        return finalValue
    }

    private fun getMetadata(request: HttpRequest<Any>): Map<String, String> {
        val headers = request.headers
        return headers
            .filter { it.key.startsWith("x-tp-") }
            .map { it.key.substring(5) to it.value.joinToString<String?>(";") }
            .toMap()
    }
}