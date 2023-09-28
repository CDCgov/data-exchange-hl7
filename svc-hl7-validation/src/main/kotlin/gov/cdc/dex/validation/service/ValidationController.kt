package gov.cdc.dex.validation.service

import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import gov.cdc.dex.metadata.HL7MessageType
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpRequest.POST
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import kotlin.jvm.optionals.getOrNull


@Controller("/")
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

    @Get(value = "/health")
    fun getRootPingResponse() : String {
        return "hello"
    }

    @Post(value = "/validation", consumes = [MediaType.TEXT_PLAIN], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary = "Action for validating HL7 Message")
    @ApiResponses(
        ApiResponse(content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode =  "400", description = "Bad Request")
    )
    @Parameters(
        Parameter(name="x-tp-message_type", `in` = ParameterIn.HEADER, description="Required. Whether the Message is a CASE message or ELR message. Current valid values: [CASE, ELR].", required = true, schema= Schema(type = "string")),
        Parameter(name="x-tp-route", `in` = ParameterIn.HEADER, description="Required for message-type == ELR. The program/area that is sending the message. Current valid values: [COVID19_ELR,PHLIP_FLU,PHLIP_VPD  ]", required = true, schema= Schema(type = "string"))
    )
    fun validate(@Body content: String, request: HttpRequest<Any>): HttpResponse<String> {
        log.info("AUDIT::Executing Validation of message....")
        val metadata = getMetadata(request)
        if (metadata["message_type"].isNullOrEmpty()) {
            log.error("Missing Header for message_type")
            return HttpResponse.badRequest("BAD REQUEST: Message Type ('CASE' or 'ELR') " +
                    "must be specified in the HTTP Header as 'x-tp-message_type'. " +
                    "Please correct the HTTP header and try again.")
        }
        if (metadata["message_type"] == HL7MessageType.ELR.name && metadata["route"].isNullOrEmpty()) {
            log.error("Missing Header for route when Message_type == ELR")
           return HttpResponse.badRequest("BAD REQUEST: ELR message must specify a route" +
                        " in the HTTP header as 'x-tp-route'. " +
                        "Please correct the HTTP header and try again.")
        }
        val resultData = this.validateMessage(content, metadata)
        return if (!resultData.startsWith("Error")) {
            log.info("message successfully redacted and validated")
            HttpResponse.ok(resultData).contentEncoding(MediaType.APPLICATION_JSON)
        } else {
            log.error(resultData)
            HttpResponse.badRequest(resultData).contentEncoding(MediaType.TEXT_PLAIN)
        }

    }

    private fun validateMessage(hl7Content: String, metadata: Map<String, String>): String {
        val redactedMessage = getRedactedContent(hl7Content, metadata)
        return if (redactedMessage.isEmpty()) {
            "Error: Redacted message is empty"
        } else if (redactedMessage.startsWith("Error")) {
            redactedMessage
        } else {
            getStructureReport(redactedMessage, metadata)
        }
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
        return try {
            val response = Flux.from(call).blockFirst()
            val message = response?.getBody(String::class.java)
            message?.getOrNull() ?: "Error: No response received from $url"
        } catch (e : Exception) {
            "Error in request to $url : ${e.message}"
        }
    }

    private fun getRedactedContent(hl7Content: String, metadata: Map<String, String>): String {
        log.info("redacting message....")
        val message = postApiRequest(redactorClient, "/api/redactorReport",
            hl7Content, metadata)
        return try {
            val json = JsonParser.parseString(message).asJsonObject
            log.info("message redacted!")
            json.get("_1").asString
        } catch (e: JsonSyntaxException) {
            message
        }

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