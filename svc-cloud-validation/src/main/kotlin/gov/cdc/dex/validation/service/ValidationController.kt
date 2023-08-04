package gov.cdc.dex.validation.service

//import gov.cdc.dex.hl7.Helper
//import cdc.xlr.structurevalidator._

import com.google.gson.*
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
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
    ): MutableHttpResponse<MutableHttpResponse<String>?>? {
        val resultData = this.validateMessage(request, content);

        return HttpResponse.ok(resultData).contentEncoding(MediaType.APPLICATION_JSON)
    }

    private fun validateMessage(request: HttpRequest<Any>, hl7Content: String): MutableHttpResponse<String>? {
        var reportData = ""

        var metaData = getMetadata(request)

        var redactedMessage = this.getRedactedContent(URL(this.redactorUrl), hl7Content, metaData)

        var structureReport = this.getReportContent(URL(this.structureUrl), redactedMessage, metaData)

        var jsonData = JsonObject()

        jsonData.add("StructureReport", JsonParser.parseString(gson.toJson(structureReport)))
        reportData = jsonData.toString()

        return HttpResponse.ok(reportData).contentEncoding(MediaType.APPLICATION_JSON)
    }

    private fun getHttpRequest(urlPath: String, messageType: String?, routeText: String?, payLoad: String): java.net.http.HttpRequest {
        val request = java.net.http.HttpRequest.newBuilder()
            request.uri(URI.create(urlPath))
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payLoad))
                .setHeader("x-tp-message_type", messageType)
                .setHeader("x-tp-route", routeText)
                .build()
        return request.build()
    }

    private fun getRedactedContent(url: URL, payLoad : String, metadata: Map<String, String>? = null): String {
        val client = HttpClient.newBuilder().build()

        val urlPath = url.toString()
        val messageType = metadata?.get("message_type")
        val routeText = metadata?.get("route")

        val request = getHttpRequest(urlPath, messageType, routeText, payLoad)

        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).body()

        val json = JsonParser().parse(response)
        var jsonObject = json.asJsonObject
        var jsonItem = jsonObject.get("_1")
        response = jsonItem.asString
        return response
    }

    private fun getReportContent(url: URL, payLoad : String, metadata: Map<String, String>? = null): String {
        val client = HttpClient.newBuilder().build()

        val urlPath = url.toString()
        val messageType = metadata?.get("message_type")

        val request = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create(urlPath))
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payLoad))
            .setHeader("x-tp-message_type", messageType)
            .build()

        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).body()

        return response
    }

    private fun getMetadata(request: HttpRequest<Any>): Map<String, String> {
        val headers = request.headers
        return headers
            .filter { it.key.startsWith("x-tp-") }
            .map { it.key.substring(5) to it.value.joinToString<String?>(";") }
            .toMap()
    }
}