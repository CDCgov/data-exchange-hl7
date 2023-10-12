package gov.cdc.dex.replay.service

import gov.cdc.dex.azure.EventHubSender

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpHeaders
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.http.MediaType

import org.slf4j.LoggerFactory
import java.util.*

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses


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
enum class MessageQueryType{
    start_date,
    end_date,
    jurisdiction,
    route
}
@Schema(description = "Request Object for Replay by file uuid")
data class RequestReplay(
    @Schema(description = "Reason for replay",example = "Message is error queued", required = true)
    val reason: String,
    @Schema(description = "User calling the API", required = true)
    val user: String,
    @Schema(description = "Start Date of file to be replayed",example = "")
    val start_date: Date?,
    @Schema(description = "End Date of file to be replayed",example = "2023-10-12T16:02:37.481Z")
    val end_date: Date?,
    @Schema(description = "Message query method ",example = "2023-10-14T16:02:37.481Z", required = true)
    val message_query: MessageQueryType
)
@Controller("/replay")
class ReplayController {
    companion object {
        val gson: Gson = GsonBuilder().serializeNulls().create()
        val evHubConnStr: String = System.getenv("EventHubConnectionString")
        private var logger = LoggerFactory.getLogger(Function::class.java.name)
    }
    @Post("/{message_uuid}", consumes=[MediaType.APPLICATION_JSON], produces =[MediaType.APPLICATION_JSON])
    @Operation(summary = "Replays HL7 Message both validated and error queued by message UUID")
    @Parameters(
        Parameter(name="message_uuid", `in` = ParameterIn.PATH, description =" Replays HL7 messages by message uuid", required=true, schema=Schema(type = "string"), example = "123e4567-e89b-12d3-a456-426655440000")
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode =  "400", description = "Bad request"),
        ApiResponse(responseCode =  "403", description = "Authorization Error"),
        ApiResponse(responseCode =  "500", description = "Internal error")
    )
    fun replayMessageUUIDController(

        @RequestBody(
            description = "Request Object for replaying message by message uuid",
            content = [Content(schema = Schema(implementation = RequestReplay::class))]
        )request: RequestReplay ):HttpResponse<String>{

        logger.info("Starting the replay of the message by message UUID $request")

        return HttpResponse.ok()
    }
    @Post("/{file_uuid}", consumes=[MediaType.APPLICATION_JSON], produces =[MediaType.APPLICATION_JSON])
    @Operation(summary = "Replays HL7 Message both validated and error queued by file UUID")
    @Parameters(
        Parameter(name="file_uuid", `in` = ParameterIn.PATH, description =" Replays HL7 messages by file uuid", required=true, schema=Schema(type = "string"),example = "123e4567-e89b-12d3-a456-426655440000")
    )
    @ApiResponses(
        ApiResponse(content = [Content(mediaType = MediaType.APPLICATION_JSON, schema = Schema(type = "string"))]),
        ApiResponse(responseCode = "200", description = "Success"),
        ApiResponse(responseCode =  "400", description = "Bad request"),
        ApiResponse(responseCode =  "403", description = "Authorization Error"),
        ApiResponse(responseCode =  "500", description = "Internal error")
    )
    fun replayFileUUIDController(@RequestBody(
        description = " Request Object for replaying messages by message uuid",
        content = [Content(schema = Schema(implementation = RequestReplay::class))]
    )request: RequestReplay ):HttpResponse<String>{

        logger.info("Starting the replay of the message by file UUID ${request.message_query}")

        return HttpResponse.ok()
    }

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

    // Supporting Fns - Query Builder
    private fun returnQueryId(identifier : String, table : String, id : String ): String {
        // Query based on Original Message with ID
        val query = "SELECT * FROM $table WHERE $identifier = $id"
        // TODO - Only original message, Replay MD should be null
        return query
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
        return jsonMessage
    }
}

