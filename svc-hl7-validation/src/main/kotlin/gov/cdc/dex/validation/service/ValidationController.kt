package gov.cdc.dex.validation.service


import com.google.gson.*
import gov.cdc.dex.metadata.HL7MessageType
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.validation.service.model.ErrorCounts
import gov.cdc.dex.validation.service.model.ErrorInfo
import gov.cdc.dex.validation.service.model.Summary
import gov.cdc.nist.validator.NistReport
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpRequest.POST
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.util.*
import kotlin.jvm.optionals.getOrNull


@Controller("/")
class ValidationController(@Client("redactor") redactorClient: HttpClient, @Client("structure") structureClient: HttpClient ) {
    private var redactorClient: HttpClient
    private var structureClient: HttpClient

    //private HttpClient client;

    companion object {
        private val log = LoggerFactory.getLogger(ValidationController::class.java.name)
        private const val UTF_BOM = "\uFEFF"

    }
    init {
        this.redactorClient = redactorClient
        this.structureClient = structureClient
    }

    @Get(value = "/heartbeat", produces = [MediaType.TEXT_PLAIN])
    @Operation(summary="Used by application startup process. Returns 'hello' if all is well.")
    @ApiResponses(
        ApiResponse(content = [Content(mediaType = "text/plain", schema = Schema(type = "string"))]),
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode =  "400", description = "Bad Request")
    )
    fun getHeartbeatPingResponse() : String {
        return "hello"
    }

    @Post(value = "validate", consumes = [MediaType.TEXT_PLAIN], produces = [MediaType.APPLICATION_JSON])
    @Operation(summary="Action for validating HL7 Message(s)", description = "Query parameters: \n\n" +
            "1. message_type - Required. Whether the Message is a CASE message or ELR message. Current valid values: [CASE, ELR].\n" +
            "2. route - Required for message-type == ELR. The program/area that is sending the message. Current valid values: [COVID19_ELR,PHLIP_FLU,PHLIP_VPD ]\n\n"
    )
    @ApiResponses(
        ApiResponse(responseCode = "200",
            description = "Success",
            content = [
                Content(
                    mediaType = "application/json",
                    schema = Schema(oneOf = [NistReport::class, Summary::class]),
                    examples = [
                        ExampleObject(name="Single Message Response", value = """{
                          "entries": {
                            "structure": [
                              {
                                "line": 14,
                                "column": 56,
                                "path": "OBX[10]-5[1].2",
                                "description": "test is not a valid Number. The format should be: [+|-]digits[.digits]",
                                "category": "Format",
                                "classification": "Error",
                                "stackTrace": null,
                                "metaData": null
                              }
                            ],
                            "content": [
                              {
                                "line": 3,
                                "column": 99,
                                "path": "OBR[1]-7[1].1",
                                "description": "DateTimeOrAll0s - If TS.1 (Time) is valued then TS.1 (Time) shall follow the date/time pattern 'YYYYMMDDHHMMSS[.S[S[S[S]]]][+/-ZZZZ]]'.",
                                "category": "Constraint Failure",
                                "classification": "Error"
                              }
                            ],
                            "value-set": []
                          },
                          "error-count": {
                            "structure": 1,
                            "value-set": 0,
                            "content": 1
                          },
                          "warning-count": {
                            "structure": 0,
                            "value-set": 0,
                            "content": 0
                          },
                          "status": "STRUCTURE_ERRORS"
                        }""" ),
                    ExampleObject(name="Batch Message Response", value = """{
                        "total_messages": 5,
                        "valid_messages": 2,
                        "invalid_messages": 3,
                        "error_counts": {
                            "total": 4,
                            "by_type": {
                                "structure": 1,
                                "content": 2,
                                "value_set": 0,
                                "other": 1
                            },
                            "by_category": {
                                "Constraint Failure": 2,
                                "Runtime Error": 1,
                                "Usage": 1
                            },
                            "by_path": {
                                "PID[1]-3[1]": 1,
                                "OBR[1]-22[1].1": 1,
                                "MSH-12": 1,
                                "PID[1]-5[1]": 1
                            },
                            "by_message": {
                                "message-1": 2,
                                "message-2": 1,
                                "message-3": 1,
                                "message-4": 0,
                                "message-5": 0
                            }
                        }
                    }
                    """)
                    ]

                )
            ]
        ),
        ApiResponse(responseCode =  "400",
            description = "Bad Request",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = ErrorInfo::class)
            )]
        )
    )
    fun validate(
            @Parameter(name="message_type",
                schema = Schema(description = "The type of data contained in the HL7 message",
                    allowableValues = ["ELR", "CASE"], required = true, type = "string"))
                @QueryValue message_type: String,
            @Parameter(name="route", schema = Schema(description = "For ELR only; the profile specification name",
                allowableValues = ["COVID19_ELR", "PHLIP_FLU", "PHLIP_VPD"], type = "string"))
                @QueryValue route:Optional<String>,
            @Body content: String, request: HttpRequest<Any>): HttpResponse<String> {
        log.info("AUDIT::Executing Validation of message....")
        val routeValue = route.orElse("")
        val metadata: HashMap<String, String> = HashMap()
        metadata["message_type"]= message_type
        metadata["route"]= routeValue

        if (message_type.isEmpty()) {
            log.error("Missing Header for message_type")
            return badRequest("BAD REQUEST: Message Type ('CASE' or 'ELR') must be specified using query parameter 'message_type'. Please try again.")
        }
        if (message_type == HL7MessageType.ELR.name && routeValue.isNullOrEmpty()) {
            log.error("Missing Header for route when Message_type == ELR")
            return badRequest("BAD REQUEST: ELR message must specify a route using query parameter 'route'. Please try again.")
        }
        // since content is a required parameter, we can be certain it has a value.
        // otherwise, 'bad request' would have been returned by Micronaut.
        val arrayOfMessages = debatchMessages(content)
        return if (arrayOfMessages.size == 1) {
            val resultData = this.validateMessage(arrayOfMessages[0], metadata)
            if (!resultData.startsWith("Error")) {
                log.info("message successfully redacted and validated")
                HttpResponse.ok(resultData).contentEncoding(MediaType.APPLICATION_JSON)
            } else {
                log.error(resultData)
                badRequest(resultData)
            }
        } else {
            val resultSummary = this.validateBatch(arrayOfMessages, metadata)
            log.info("batch summary created successfully")
            HttpResponse.ok(resultSummary).contentEncoding(MediaType.APPLICATION_JSON)
        }

    }

    private fun badRequest(responseMessage: String) : HttpResponse<String> {
        val error = ErrorInfo(description = responseMessage)
        return HttpResponse.badRequest(JsonHelper.gson.toJson(error)).contentEncoding(MediaType.APPLICATION_JSON)
    }

    private fun validateBatch(arrayOfMessages: ArrayList<String>, metadata: Map<String, String>): String {
        val mapOfResults = mutableMapOf<String, String>()
        arrayOfMessages.forEachIndexed { index, message ->
            val result = validateMessage(message, metadata)
            mapOfResults.putIfAbsent("message-${index + 1}", result)
        }
        return prepareSummaryFromMap(mapOfResults)
    }

    private fun prepareSummaryFromMap(mapOfResults: Map<String, String>) : String {
        var runtimeErrorCount = 0
        var structureErrorCount = 0
        var contentErrorCount = 0
        var valueSetErrorCount = 0
        val countsByMessage = mutableMapOf<String, Int>()
        var validMessageCount = 0
        var invalidMessageCount = 0
        val entries = mutableListOf<JsonElement>()
        mapOfResults.forEach { (messageId, report) ->
            // each 'report' with be either a NIST report (JSON) or a runtime error (plain text)
            if (report.startsWith("Error")) {
                runtimeErrorCount++
                invalidMessageCount++
                // extract the path that caused the error, if it exists
                val regex = "[A-Z]{3}-[0-9]{1,2}".toRegex()
                val path = regex.find(report)?.value + ""
                val error = ErrorInfo (description = report, path = path).toJsonElement()
                countsByMessage.putIfAbsent(messageId, 1)
                entries.add(error)
            } else {
                val reportJson = JsonParser.parseString(report).asJsonObject
                if (JsonHelper.getValueFromJson("status", reportJson).asString == "VALID_MESSAGE") {
                    // valid message has 0 errors
                    validMessageCount++
                    countsByMessage.putIfAbsent(messageId, 0)
                } else {
                    invalidMessageCount++
                    val structure = getListOfErrors("entries.structure", reportJson)
                    val content = getListOfErrors("entries.content", reportJson)
                    val valueSet = getListOfErrors("entries.value-set", reportJson)
                    structureErrorCount += structure.size
                    contentErrorCount += content.size
                    valueSetErrorCount += valueSet.size
                    countsByMessage.putIfAbsent(messageId, structure.size + content.size + valueSet.size)
                    entries.addAll(structure + content + valueSet)
                }
            } // .if
        } //.forEach
        val countsByCategory = entries.groupingBy { JsonHelper.getValueFromJson("category", it).asString }.eachCount()
        val countsByPath = entries.groupingBy { JsonHelper.getValueFromJson("path", it).asString }.eachCount()

        val summary = Summary(
            totalMessages = mapOfResults.size,
            validMessages = validMessageCount,
            invalidMessages = invalidMessageCount,
            errors = ErrorCounts(
                totalErrors = runtimeErrorCount + structureErrorCount + contentErrorCount + valueSetErrorCount,
                errorsByType = mapOf( "structure" to structureErrorCount,
                    "content" to contentErrorCount,
                    "value_set" to valueSetErrorCount,
                    "other" to runtimeErrorCount),
                errorsByCategory = countsByCategory,
                errorsByPath = countsByPath,
                errorsByMessage = countsByMessage
            )
        )
        return JsonHelper.gson.toJson(summary)
    }

    private fun getListOfErrors(jsonPath : String, report: JsonObject) : List<JsonElement> {
        return JsonHelper.getValueFromJson(jsonPath, report)
            .asJsonArray.filter { JsonHelper.getValueFromJson("classification", it ).asString == "Error" }
    }

    private fun debatchMessages(messages : String) : ArrayList<String> {
        val messageLines = messages.lines()
        val currentLinesArr = arrayListOf<String>()
        val messagesArr = arrayListOf<String>()
        var mshCount = 0
        messageLines.forEach { line ->
            val lineClean = line.trim().let { if (it.startsWith(UTF_BOM)) it.substring(1) else it }
            if (lineClean.startsWith("FHS") ||
                lineClean.startsWith("BHS") ||
                lineClean.startsWith("BTS") ||
                lineClean.startsWith("FTS")) {

                // batch line --Nothing to do here
            } else if (lineClean.isNotEmpty()) {
                if (lineClean.startsWith("MSH|")) {
                    mshCount++
                    if (mshCount > 1) {
                        messagesArr.add(currentLinesArr.joinToString("\n"))
                    }
                    currentLinesArr.clear()
                } // .if
                currentLinesArr.add(lineClean)
            }
        }
        if (currentLinesArr.isNotEmpty()) {
            messagesArr.add(currentLinesArr.joinToString("\n"))
        }
        return messagesArr
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

}