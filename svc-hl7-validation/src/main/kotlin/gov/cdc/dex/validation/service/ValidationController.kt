package gov.cdc.dex.validation.service


import com.google.gson.*
import gov.cdc.dex.metadata.HL7MessageType
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.validation.service.model.ErrorCounts
import gov.cdc.dex.validation.service.model.ErrorInfo
import gov.cdc.dex.validation.service.model.Summary
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpRequest.POST
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.swagger.v3.oas.annotations.Operation
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
        private const val UTF_BOM = "\uFEFF"

    }
    init {
        this.redactorClient = redactorClient
        this.structureClient = structureClient
    }

    @Get(value = "/")
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
    fun validate(@QueryValue message_type: String, @Nullable @QueryValue route: String, @Body content: String, request: HttpRequest<Any>): HttpResponse<String> {
        log.info("AUDIT::Executing Validation of message....")
        var metadata: HashMap<String, String> = HashMap()
        metadata["message_type"]= message_type
        metadata["route"]= route
        if (message_type.isNullOrEmpty()) {
            log.error("Missing Header for message_type")
            return HttpResponse.badRequest("BAD REQUEST: Message Type ('CASE' or 'ELR') " +
                    "must be specified in the HTTP Header as 'x-tp-message_type'. " +
                    "Please correct the HTTP header and try again.")
        }
        if (message_type == HL7MessageType.ELR.name && route.isNullOrEmpty()) {
            log.error("Missing Header for route when Message_type == ELR")
           return HttpResponse.badRequest("BAD REQUEST: ELR message must specify a route" +
                        " in the HTTP header as 'x-tp-route'. " +
                        "Please correct the HTTP header and try again.")
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
                HttpResponse.badRequest(resultData).contentEncoding(MediaType.TEXT_PLAIN)
            }
        } else {
            val resultSummary = this.validateBatch(arrayOfMessages, metadata)
            log.info("batch summary created successfully")
            HttpResponse.ok(resultSummary).contentEncoding(MediaType.APPLICATION_JSON)
        }

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