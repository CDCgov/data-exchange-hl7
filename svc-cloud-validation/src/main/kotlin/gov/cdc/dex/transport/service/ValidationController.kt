package gov.cdc.dex.validation.service

//import gov.cdc.dex.hl7.Helper
//import cdc.xlr.structurevalidator._

import com.google.gson.*
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.io.*
import java.net.*

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

        val byteArray : ByteArray
        var resultData = ""
        val conn = url.openConnection() as HttpURLConnection
        conn.disconnect()
        conn.requestMethod = "POST"
        conn.setRequestProperty("x-tp-message_type", metadata?.get("x-tp-message_type"))
        conn.setRequestProperty("x-tp-route", metadata?.get("x-tp-route"))
        conn.useCaches = false
        conn.doOutput = true
        conn.connect()
        //DataOutputStream(conn.outputStream).use { wr -> wr.write(byteArray) }
        //val inputStream = conn.inputStream
        DataOutputStream(conn.getOutputStream()).use { it.writeBytes(payLoad) }
        BufferedReader(InputStreamReader(conn.getInputStream())).use { bf ->
            var line: String?
            while (bf.readLine().also { line = it } != null) {
                println(line)
            }
        }

        return resultData
    }


    private fun getContent_01(url: URL, payLoad : String, metadata: Map<String, String>? = null): String {
        //"https://ocio-ede-dev-hl7-structure-validator.azurewebsites.net/api/structure"

        val response = HttpRequest.POST(url.toString(), payLoad).headers.add("x-tp-message_type", "CASE")
        val content = (response as MutableHttpRequest<*>).contentType(MediaType.TEXT_PLAIN_TYPE)
        val result = response.body
        val responseData = content.toString()
        return result.toString()
        //val flowable = httpClient.retrieve(req)
        //return req as CompletableFuture<String>
    }
    private fun getContent_orig(url: URL, payLoad : String, metadata: Map<String, String>? = null): String {
        val sb = StringBuilder()
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET" //"POST"
        conn.doOutput = true
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("x-tp-message_type", metadata?.get("message_type"))
        conn.setRequestProperty("x-tp-route", metadata?.get("x-tp-route"))
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

    private fun getMetadata(request: HttpRequest<Any>): Map<String, String> {
        val headers = request.headers
        return headers
            .filter { it.key.startsWith("x-tp-") }
            .map { it.key.substring(5) to it.value.joinToString<String?>(";") }
            .toMap()
    }
}