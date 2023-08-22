package gov.cdc.dex.validation.service

import com.google.gson.JsonParser
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
/**
 *
 * @Created - 6/23/23
 * @Author USY6@cdc.gov
 */
@Controller("/validation")
class ValidationController {
    companion object {
        private val log = LoggerFactory.getLogger(ValidationController::class.java.name)

        val redactorUrl:String = System.getenv("REDACTOR_URL") + "/api/redactorReport"
        val structureUrl = System.getenv("STRUCTURE_URL") + "/api/structure"
    }

    init {
        log.info("Redactor set for $redactorUrl")
        log.info("Structure Validator set for $structureUrl")
    }

    @Post(value = "/", consumes = [MediaType.TEXT_PLAIN], produces = [MediaType.APPLICATION_JSON])
    fun validate(@Body content: String, request: HttpRequest<Any>): HttpResponse<String> {
        log.info("AUDIT::Executing Validation of message....")
        val metadata = getMetadata(request)
        if (metadata["message_type"].isNullOrEmpty()) {
            log.error("Missing Header for message_type")
            //TODO::Convert Error results into Json
            return HttpResponse.badRequest("BAD REQUEST: Message Type ('CASE' or 'ELR') " +
                    "must be specified in the HTTP Header as 'x-tp-message_type'. " +
                    "Please correct the HTTP header and try again.")
        }
        if (metadata["message_type"] == "ELR" && metadata["route"].isNullOrEmpty()) {
            log.error("Missing Header for route when Message_type == ELR")
           return HttpResponse.badRequest("BAD REQUEST: ELR message must specify a route" +
                        " in the HTTP header as 'x-tp-route'. " +
                        "Please correct the HTTP header and try again.")
        }
        val resultData = this.validateMessage(content, metadata)
        log.info("message successfully redacted and validated")
        return HttpResponse.ok(resultData).contentEncoding(MediaType.APPLICATION_JSON)
    }

    private fun validateMessage(hl7Content: String, metadata: Map<String, String>?): String {
        val redactedMessage = this.getRedactedContent(hl7Content, metadata)
        return getStructureReport(redactedMessage, metadata)
    }

    private fun createHttpRequest(urlPath: String, metadata: Map<String, String>?, payLoad: String): java.net.http.HttpRequest {
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

        request.uri(URI.create(urlPath))
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payLoad))
            .setHeader("x-tp-message_type", messageType)

        if ("ELR" == messageType)
            request.setHeader("x-tp-route", routeText)

        return request.build()
    }

    private fun getRedactedContent(payLoad: String, metadata: Map<String, String>? = null): String {
        log.info("redacting message....")
        val client = HttpClient.newBuilder().build()
        val request = createHttpRequest(redactorUrl, metadata, payLoad)
        val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).body()
        val json = JsonParser.parseString(response).asJsonObject
        log.info("message redacted!")
        return json.get("_1").asString
    }

    private fun getStructureReport(payLoad: String, metadata: Map<String, String>?): String {
        log.info("Validating message...")
        val client = HttpClient.newBuilder().build()
        val request = createHttpRequest(structureUrl, metadata, payLoad)
        val structReport = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).body()
        log.info("message Validated")
        return structReport
    }

    private fun getMetadata(request: HttpRequest<Any>): Map<String, String> {
        val headers = request.headers
        return headers
            .filter { it.key.startsWith("x-tp-") }
            .map { it.key.substring(5) to it.value.joinToString<String?>(";") }
            .toMap()
    }

}