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
import jakarta.inject.Inject
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux


/**
 *
 * @Created - 6/23/23
 * @Author USY6@cdc.gov
 */
@Controller("/validation")
class ValidationController {
    companion object {
        private val log = LoggerFactory.getLogger(ValidationController::class.java.name)
        @field:Client("redactor") @Inject lateinit var redactorClient : HttpClient
        @field:Client("structure") @Inject lateinit var structureClient : HttpClient

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
        val resultData = this.validateMessage(content, request)
        log.info("message successfully redacted and validated")
        return HttpResponse.ok(resultData).contentEncoding(MediaType.APPLICATION_JSON)
    }

    private fun validateMessage(hl7Content: String, request: HttpRequest<Any>): String {
        val redactedMessage = this.getRedactedContent(hl7Content, request)
        return getStructureReport(redactedMessage, request)
    }

    private fun getRedactedContent(hl7Content: String, request: HttpRequest<Any>): String {
        log.info("redacting message....")

        val call =
            redactorClient.exchange(
                POST("/", hl7Content)
                    .headers { request.headers },
                String::class.java
            )
        val response = Flux.from(call).blockFirst()
        val message = response.getBody(String::class.java)
        val json = JsonParser.parseString(message.toString()).asJsonObject
        log.info("message redacted!")
        return json.get("_1").asString
    }

    private fun getStructureReport(hl7Content: String, request: HttpRequest<Any>): String {
        log.info("Validating message...")
        val structReport =
            structureClient.exchange(
                POST("/", hl7Content )
                    .headers { request.headers },
                String::class.java
            )
        log.info("message Validated")
        return structReport.toString()
    }

    private fun getMetadata(request: HttpRequest<Any>): Map<String, String> {
        val headers = request.headers
        return headers
            .filter { it.key.startsWith("x-tp-") }
            .associate { it.key.substring(5) to it.value.joinToString<String?>(";") }
    }

}