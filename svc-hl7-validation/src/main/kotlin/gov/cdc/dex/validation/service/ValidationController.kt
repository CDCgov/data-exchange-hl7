package gov.cdc.dex.validation.service

import com.google.gson.JsonParser
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpRequest.POST
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import kotlin.jvm.optionals.getOrNull


@Controller("/validation")
class ValidationController(@Client("redactor") redactorClient: HttpClient, @Client("structure") structureClient: HttpClient ) {
    private var redactorClient: HttpClient
    private var structureClient: HttpClient

    companion object {
        private val log = LoggerFactory.getLogger(ValidationController::class.java.name)

    }
    init {
        this.redactorClient = redactorClient
        this.structureClient = structureClient
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

    private fun validateMessage(hl7Content: String, metadata: Map<String, String>): String {
        val redactedMessage = this.getRedactedContent(hl7Content, metadata)
        return getStructureReport(redactedMessage, metadata)
    }

    private fun postApiRequest(client: HttpClient, url: String, bodyContent: String, metadata: Map<String, String>) : String {
        val call =
            client.exchange(
                POST(url, bodyContent)
                    .contentType(MediaType.TEXT_PLAIN)
                    .header("x-tp-message_type", metadata["message_type"])
                    .header("x-tp-route", metadata["route"] ?: ""),
                String::class.java
            )
        val response = Flux.from(call).blockFirst()
        val message = response?.getBody(String::class.java)
        return message?.getOrNull() ?: ""
    }

    private fun getRedactedContent(hl7Content: String, metadata: Map<String, String>): String {
        log.info("redacting message....")
        val message = postApiRequest(redactorClient, "/api/redactorReport",
            hl7Content, metadata)
        val json = JsonParser.parseString(message).asJsonObject
        log.info("message redacted!")
        return json.get("_1").asString
    }

    private fun getStructureReport(hl7Content: String, metadata: Map<String, String>): String {
        log.info("Validating message...")
        val structReport = postApiRequest(structureClient, "/api/structure",
            hl7Content, metadata)
        log.info("message Validated")
        return structReport
    }

    private fun getMetadata(request: HttpRequest<Any>): Map<String, String> {
        val headers = request.headers
        return headers
            .filter { it.key.startsWith("x-tp-") }
            .associate { it.key.substring(5) to (it.value.firstOrNull() ?: "") }
    }

}