package gov.cdc.dex.replay.service

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.metadata.*
import org.slf4j.LoggerFactory
import java.util.*
import java.time.Instant

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.micronaut.context.annotation.Requires

/**
 *
 * @Created - 8/15/23
 * @Author UUX3@cdc.gov
 */

data class UniqueData(
    val id: String,
    val reason: String,
    val route: String
)
data class CombinationData(
    val startDate: Date?,
    val endDate: Date?,
    val jurisdiction: String?,
    val route: String,
    val reason: String
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
// @Requires(property = "micronaut.security.enabled", value = "false")
class ReplayController {

    companion object {
        val gson: Gson = GsonBuilder().serializeNulls().create()
        val evHubConnStr: String = System.getenv("EventHubConnectionString")
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    }

    @Post("/messages")
    fun handleMessage(@Body messageRequest: UniqueData, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling message: ${messageRequest.id} at location: $location")
        // Build and Query DB
        val query = returnQueryId("message_uuid", "Test", messageRequest.id)
        // TODO - Run Query

        // Create replay md
        val replayMD = cleanReplayMetadata(null, query, messageRequest.reason, messageRequest.route)
        // Build Message
        // buildEventMD()

        // Send Event Hub Message
        val evResult = prepareAndSend( gson.toJson(replayMD), messageRequest.route)
        return HttpResponse.ok("Received message id: ${messageRequest.id}")
    }

    @Post("/files")
    fun handleFile(@Body messageRequest: UniqueData, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling file: ${messageRequest.id} at location: $location")
        // Build and Query DB
        val query = returnQueryId("file_uuid", "Test", messageRequest.id)
        // TODO - Run Query
        // Create replay md
        val replayMD = cleanReplayMetadata(null, query, messageRequest.reason, messageRequest.route)
        // Build Message
        // buildEventMD()

        // Send Event Hub Message
        val evResult = prepareAndSend( gson.toJson(replayMD), messageRequest.route)

        return HttpResponse.ok("Received file id: ${messageRequest.id}")
    }

    @Post("/combo")
    fun handleComboMessage(@Body comboData: CombinationData, headers: HttpHeaders): HttpResponse<String> {
        val location = headers["location"]
        println("Handling combo message: $comboData at location: $location")
        // Build and Query DB
        val query = queryCombo(comboData, "Test")
        // TODO - Run Query
        // Create replay md
        val replayMD = cleanReplayMetadata(comboData, query)
        // Build Message
        // buildEventMD()

        // Send Event Hub Message
        val evResult = prepareAndSend( gson.toJson(replayMD), comboData.route)

        return HttpResponse.ok("Received combination data: $comboData")
    }

    // Supporting Fns - Query Builder
    private fun returnQueryId(identifier : String, table : String, id : String ): String {
        // Query based on Original Message with ID
        val query = "SELECT * FROM $table WHERE $identifier = $id"
        // TODO - Only original message, Replay MD should be null
        return query
    }
    private fun queryCombo(comboData: CombinationData?, table : String ) : String {
        var conditionals = mutableListOf<String>()

        // Double check columns in CosmosDB
        comboData?.startDate?.let { conditionals.add("startdate='${comboData.startDate}'")}
        comboData?.endDate?.let { conditionals.add("enddate='${comboData.endDate}'")}
        comboData?.jurisdiction?.let { conditionals.add("jurisdiction='${comboData.jurisdiction}'")}

        if (conditionals.isEmpty()){
            // No Query values, alert user to provide values
            return "ERROR"
        }

        val action = conditionals.joinToString { " AND " }
        return "SELECT * FROM $table WHERE $action"
    }
    // Supporting Fns - Replay Metadata
    private fun cleanReplayMetadata(comboData: CombinationData?, queryFilter: String, reasonMsg: String = "", routeMsg: String = "" ) : ReplayMD {

        var replayMD = ReplayMD()

        if (comboData == null) {
            replayMD.apply {
                replayTimestamp = Date()
                reason = reasonMsg
                startingProcess = routeMsg // TODO - Route Default?
                filter = queryFilter
            }
        } else {
            replayMD.apply {
                replayTimestamp = Date()
                reason = comboData.reason
                startingProcess = comboData.route
                filter = queryFilter
            }
        }

        println("Replay Metadata: $replayMD")
        return replayMD
    }
    private fun buildEventMD( queryResult : String, replayMD : String) : JsonObject{
        // TODO - Take Event and append replay metadata
        val eventObj : JsonObject = JsonParser.parseString(queryResult) as JsonObject
        val replayObj : JsonObject = JsonParser.parseString(replayMD) as JsonObject

        eventObj.add("replay", replayObj)

        return eventObj
    }
    // Supporting Fns - Send to Event Hub Topic
    private fun prepareAndSend(msg: String, eventHubName: String): String? {
        // Receive Raw Result from Cosmos
        // Convert to JsonObject
        println("PrepareAndSend() - $msg to Event Hub Topic: $eventHubName")
        val msgEvent: JsonObject = JsonParser.parseString(msg) as JsonObject
        val jsonMessage = gson.toJson(msgEvent)

        // TODO - Check if Event Hub Topic is valid?
        val eventHubSender = EventHubSender(evHubConnStr)

        eventHubSender.send(evHubTopicName=eventHubName, message=jsonMessage)
        //println(msgEvent)
        return jsonMessage
    }
}

