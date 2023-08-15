package gov.cdc.dex.replay.service

import com.google.gson.JsonParser
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
/**
 *
 * @Created - 8/15/23
 * @Author UUX6@cdc.gov
 */
@Controller("/replay")
//@Requires(property = "micronaut.security.enabled", value = "false")
class ReplayController {

    @Post("/messages")
    fun handleMessage(@Body messageId: String, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling message: $messageId at location: $location")
        // Implement your handling logic here
        return HttpResponse.ok("Received message id: $messageId")
    }

    @Post("/messages/combo")
    fun handleComboMessage(@Body comboData: CombinationData, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling combo message: $comboData at location: $location")
        // Implement your handling logic here
        return HttpResponse.ok("Received combination data: $comboData")
    }

    @Post("/files")
    fun handleFile(@Body fileId: String, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling file: $fileId at location: $location")
        // Implement your handling logic here
        return HttpResponse.ok("Received file id: $fileId")
    }

    @Post("/files/combo")
    fun handleComboFile(@Body comboData: CombinationData, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling combo file: $comboData at location: $location")
        // Implement your handling logic here
        return HttpResponse.ok("Received combination data: $comboData")
    }
}

data class CombinationData(val date: String, val jurisdiction: String, val route: String)