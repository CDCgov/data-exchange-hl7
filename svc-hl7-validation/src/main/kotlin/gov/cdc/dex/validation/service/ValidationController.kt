package gov.cdc.dex.validation.service

import com.fasterxml.jackson.databind.JsonMappingException
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.validation.service.model.ErrorCounts
import gov.cdc.dex.validation.service.model.ErrorInfo
import gov.cdc.dex.validation.service.model.Summary

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpRequest.POST
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
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
    fun validate(@Body content: String, request: HttpRequest<Any>): HttpResponse<String> {
        log.info("AUDIT::Executing Validation of message....")
        val metadata = getMetadata(request)
        if (metadata["message_type"].isNullOrEmpty()) {
            log.error("Missing Header for message_type")
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
        val arrayOfResults = arrayListOf<String>()
        arrayOfMessages.forEach { message ->
            val result = validateMessage(message, metadata)
            arrayOfResults.add(result)
        }
        return prepareSummary(arrayOfResults)
    }

    private fun prepareSummary(arrayOfResults : ArrayList<String>) : String {
        // each result will either start with "Error" or be a JSON report
        val (runtimeErrors, structureReports) = arrayOfResults.partition { it.startsWith("Error") }
        // are there any entries that are not reports? If so, need to know where they are in the list
        val indexedRuntimeErrors = arrayOfResults.mapIndexedNotNull { index, s ->
            if (!s.startsWith("Error")) null else mapOf(index to s)
        }
        val structureJsons = structureReports.map { JsonParser.parseString(it).asJsonObject }
        val (valid, invalid) = structureJsons.partition {
            JsonHelper.getValueFromJson("status", it).asString == "VALID_MESSAGE" }

        val structureErrors = invalid.sumOf {
            JsonHelper.getValueFromJson("error-count.structure", it).asInt }
        val valueSetErrors = invalid.sumOf {
            JsonHelper.getValueFromJson("error-count.value-set", it).asInt }
        val contentErrors = invalid.sumOf {
            JsonHelper.getValueFromJson("error-count.content", it).asInt }
        val totalErrors = runtimeErrors.size + structureErrors + valueSetErrors + contentErrors

        val entries = mutableListOf<JsonElement>()
        val entriesByReport = mutableListOf<List<JsonElement>>()
        structureJsons.forEach { report ->
            val structure = JsonHelper.getValueFromJson("entries.structure", report).asJsonArray
            val content = JsonHelper.getValueFromJson("entries.content", report).asJsonArray
            val valueSet = JsonHelper.getValueFromJson("entries.value-set", report).asJsonArray
            val reportEntries = structure.plus(content).plus(valueSet).toList()
            entriesByReport.add(reportEntries)
            log.info("entries by report size: ${entriesByReport.size}")
            entries.addAll(reportEntries)
        }
        if (indexedRuntimeErrors.isNotEmpty()) {
            // add the runtime errors
            indexedRuntimeErrors.forEach { errMap ->
                errMap.keys.map {
                    val desc = errMap[it].toString()
                    val regex = "[A-Z]{3}-[0-9]{1,2}".toRegex()
                    val path = regex.find(desc)?.value + ""
                    val error = ErrorInfo (description = desc, path = path).toJsonElement()
                    entriesByReport.add(it, listOf(error))
                    entries.add(error)
                 }

            }
        }
        val errorEntries = entries.filter { JsonHelper.getValueFromJson("classification", it ).asString == "Error" }
        val categories = errorEntries.groupingBy { JsonHelper.getValueFromJson("category", it).asString }.eachCount()
        val paths = errorEntries.groupingBy { JsonHelper.getValueFromJson("path", it).asString }.eachCount()
        val countsByMessage = mutableMapOf<String, Int>()
        entriesByReport.forEachIndexed { index, jsonElements ->
            val msgErrors = jsonElements.filter { JsonHelper.getValueFromJson("classification", it).asString == "Error" }
            countsByMessage.putIfAbsent("message-${index + 1}", msgErrors.size)
        }

        val summary = Summary(
            totalMessages = arrayOfResults.size,
            validMessages = valid.size,
            invalidMessages = invalid.size + runtimeErrors.size,
            errors = ErrorCounts(
                totalErrors = totalErrors,
                errorsByType = mapOf( "structure" to structureErrors,
                                        "content" to contentErrors,
                                        "value_set" to valueSetErrors,
                                        "other" to runtimeErrors.size),
                errorsByCategory = categories,
                errorsByPath = paths,
                errorsByMessage = countsByMessage.toMap()
            )
        )
        return JsonHelper.gson.toJson(summary)
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

    private fun getMetadata(request: HttpRequest<Any>): Map<String, String> {
        val headers = request.headers
        return headers
            .filter { it.key.startsWith("x-tp-") }
            .associate { it.key.substring(5) to (it.value.firstOrNull() ?: "") }
    }

}