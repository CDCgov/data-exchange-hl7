package gov.cdc.dex.replay.service

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import java.util.Date

/**
 *
 * @Created - 8/15/23
 * @Author UUX3@cdc.gov
 */

data class CombinationData(
    val startDate: Date?,
    val endDate: Date?,
    val jurisdiction: String?,
    val route: String?
)

data class ReplayMD(
    var replayTimestamp: Date?,
    var reason: String?,
    var startingProcess: String?,
    var filter: String?
) {
    constructor() : this(null, null, null, null)
}


@Controller("/replay")
//@Requires(property = "micronaut.security.enabled", value = "false")
class ReplayController {

    @Post("/messages")
    fun handleMessage(@Body messageId: String, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling message: $messageId at location: $location")
        // Build and Query DB
        val query = returnQueryId("message_uuid", "Test", messageId)
        // If response, attached replay md
        val replayMD = cleanReplayMetadata(null, query)
        // Send Event Hub Message
        return HttpResponse.ok("Received message id: $messageId")
    }

    @Post("/messages/combo")
    fun handleComboMessage(@Body comboData: CombinationData, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling combo message: $comboData at location: $location")
        // Build and Query DB
        val query = queryCombo(comboData, "Test")
        // If response, attached replay md
        val replayMD = cleanReplayMetadata(comboData, query)
        // Send Event Hub Message
        return HttpResponse.ok("Received combination data: $comboData")
    }

    @Post("/files")
    fun handleFile(@Body fileId: String, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling file: $fileId at location: $location")
        // Build and Query DB
        val query = returnQueryId("file_uuid", "Test", fileId)
        // If response, attached replay md
        val replayMD = cleanReplayMetadata(null, query)
        // Send Event Hub Message
        return HttpResponse.ok("Received file id: $fileId")
    }

    @Post("/files/combo")
    fun handleComboFile(@Body comboData: CombinationData, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling combo file: $comboData at location: $location")
        // Build and Query DB
        val query = queryCombo(comboData, "Test")
        // If response, attached replay md
        val replayMD = cleanReplayMetadata(comboData, query)

        // Send Event Hub Message

        return HttpResponse.ok("Received combination data: $comboData")
    }

    private fun returnQueryId(identifier : String, table : String, Id : String ): String {
        val query = "SELECT * FROM $table WHERE $identifier = $Id"

        return query
    }

    private fun queryCombo(comboData: CombinationData?, table : String ) : String {
        var conditionals = mutableListOf<String>()

        // Double check columns in CosmosDB
        comboData?.startDate?.let { conditionals.add("startdate='$it'")}
        comboData?.endDate?.let { conditionals.add("enddate='$it'")}
        comboData?.jurisdiction?.let { conditionals.add("jurisdiction='$it'")}

        if (conditionals.isEmpty()){
            // No Query values, alert user to provide values
            return "ERROR"
        }

        val action = conditionals.joinToString { " AND " }
        return "SELECT * FROM $table WHERE $action"
    }
    private fun cleanReplayMetadata(comboData: CombinationData?, queryFilter: String = "TBD") : ReplayMD {

        var replayMD = ReplayMD()

        if (comboData == null) {
            replayMD.apply {
                replayTimestamp = Date()
                reason = "blah blah"
                startingProcess = "Route"
                filter = queryFilter
            }
        } else {
            replayMD.apply {
                replayTimestamp = Date()
                reason = "blah blah"
                startingProcess = comboData.route
                filter = queryFilter
            }
        }

        println("Replay Metadata: $replayMD")
        return replayMD
    }

    fun sendEventHub( topicName : String, eventText : String){
        println("Sending Event Value $eventText")
        println("To Event Topic: $topicName")
    }
}

