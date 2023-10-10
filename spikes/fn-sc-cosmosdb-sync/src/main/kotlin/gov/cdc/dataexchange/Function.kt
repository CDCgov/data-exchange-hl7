package gov.cdc.dataexchange

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.*
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dataexchange.client.CosmosDBClient.Companion.ConnectionFactory.closeConnectionShutdownHook
import gov.cdc.dataexchange.Helper.Companion.checkRequest
import gov.cdc.dataexchange.Helper.Companion.updateEventTimestamp
import gov.cdc.dataexchange.client.CosmosDBClient
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper.getValueFromJson
import org.slf4j.LoggerFactory

/**
 * Azure Function implementations.
 */
class Function {

    init {
        closeConnectionShutdownHook()
    }

    companion object {
        private var totalRecordCount = 0
        private var totalErrCount = 0
        private var totalRuntime: Long = 0
        private val logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    }

    // Synchronize the incoming messages from EventHub to CosmosDB.
    @FunctionName("upsert-eventhub")
    fun upsertEventHub(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%"
        )
        message: List<String>
    ) {
        val startTime = System.currentTimeMillis()
        val insertedRecords: MutableList<String> = mutableListOf()
        var recordCount = 0
        var errCount = 0
        try {
            message.forEachIndexed { msgIndex: Int, singleMessage: String? ->
                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                val recordId = getValueFromJson("id", inputEvent).asString

                try {
                    logger.info("[${msgIndex + 1}] inputEvent: $inputEvent")
                    if(CosmosDBClient.upsert(inputEvent)) {
                        insertedRecords.add("[id: $recordId]")
                        recordCount++
                        totalRecordCount++
                    } else {
                        insertedRecords.add("FAIL: [id: $recordId]")
                        errCount++
                        totalErrCount
                    }
                } catch (ex: Exception) {
                    logger.error("Error inserting item: [id: $recordId]", ex)
                }
            }
        } catch (e: Exception) {
            logger.error("Error occurred.", e)
        }
        totalRuntime += (System.currentTimeMillis() - startTime)
        logger.info("$recordCount records inserted: $insertedRecords")
        logger.info("$errCount failed")
        logger.info("[Duration: ${System.currentTimeMillis() - startTime}ms]")
        logger.info("total records: $totalRecordCount")
        logger.info("total errors: $totalErrCount")
        logger.info("total runtime: $totalRuntime")
    }

    // Upsert a record
    @FunctionName("upsert-record")
    fun upsertRecord(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>
    ): HttpResponseMessage {
        val startTime = System.currentTimeMillis()
        val message = request.body?.get().toString()
        return try {
            val inputEvent = JsonParser.parseString(message) as JsonObject
            logger.info("inputEvent: $inputEvent")
            if(CosmosDBClient.upsert(inputEvent)) {
                logger.info("[runtime: ${System.currentTimeMillis() - startTime}ms]")
                request.createResponseBuilder(HttpStatus.OK).body("SUCCESS:\n" +
                        "[message_uuid: ${inputEvent["message_uuid"]}]")
                    .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                    .build()
            } else {
                logger.info("[Duration: ${System.currentTimeMillis() - startTime}ms]")
                request.createResponseBuilder(HttpStatus.UNPROCESSABLE_ENTITY).body("FAIL: Unable to upsert record\n" +
                        "[message_uuid: ${inputEvent["message_uuid"]}]")
                    .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                    .build()
            }
        } catch (e: Exception) {
            logger.error("Error upserting record", e)
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("FAIL:\n" +
                    "[message: ${e.message}]")
                .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                .build()
        }
    }

    // Deletes record with provided record id and partition key.
    @FunctionName("delete-record")
    fun deleteRecord(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>
    ): HttpResponseMessage {
        val startTime = System.currentTimeMillis()
        if(!checkRequest(request)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("FAIL: Missing required headers (id or partition-key)")
                .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                .build()
        }
        val recordId = request.headers["id"]!!
        val partitionKey = request.headers["partition-key"]!!
        return try {
            if(CosmosDBClient.delete(recordId, partitionKey)) {
                request.createResponseBuilder(HttpStatus.OK).body("SUCCESS: Record deleted\n" +
                        "[id=$recordId]\n" +
                        "[partition-key=$partitionKey]")
                    .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                    .build()
            } else {
                request.createResponseBuilder(HttpStatus.UNPROCESSABLE_ENTITY).body("FAIL: Unable to delete record\n" +
                        "[id=$recordId]\n" +
                        "[partition-key=$partitionKey]")
                    .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                    .build()
            }
        } catch (e: Exception) {
            logger.error("Error deleting record [id=$recordId]", e)
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("FAIL:\n" +
                    "[message: ${e.message}]\n" +
                    "[id=$recordId]\n" +
                    "[partition-key=$partitionKey]")
                .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                .build()
        }
    }

    // Updates metadata/provenance/event_timestamp value in event message
    @FunctionName("update-timestamp")
    fun updateTimestamp(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>
    ): HttpResponseMessage {
        val startTime = System.currentTimeMillis()
        val timestamp = Date().toIsoString()
        if(!checkRequest(request)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("FAIL: Missing required headers (id or partition-key)")
                .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                .build()
        }
        val recordId = request.headers["id"]!!
        val partitionKey = request.headers["partition-key"]!!
        return try {
            val record = CosmosDBClient.read(recordId, partitionKey)
            logger.info("Updating record...")
            val updatedEventData = updateEventTimestamp(JsonParser.parseString(record) as JsonObject, timestamp)

            if (CosmosDBClient.upsert(updatedEventData)) {
                request.createResponseBuilder(HttpStatus.OK).body("SUCCESS: Record timestamp updated\n" +
                        "[id=$recordId]\n" +
                        "[partition-key=$partitionKey]")
                    .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                    .build()

            } else {
                request.createResponseBuilder(HttpStatus.UNPROCESSABLE_ENTITY).body("FAIL: Unable to upsert record\n" +
                        "[id=$recordId]\n" +
                        "[partition-key=$partitionKey]")
                    .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                    .build()
            }
        } catch (e: Exception) {
            logger.error("Error updating timestamp for record [id=$recordId]", e)
            request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("FAIL:\n" +
                    "[message: ${e.message}]\n" +
                    "[id=$recordId]\n" +
                    "[partition-key=$partitionKey]")
                .header("runtime", "${System.currentTimeMillis() - startTime}ms")
                .build()
        }
    }

    @FunctionName("reset")
    fun reset(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>
    ): HttpResponseMessage {
        totalRuntime = 0
        totalRecordCount = 0
        totalErrCount = 0
        logger.info("total runtime: $totalRuntime, total records: $totalRecordCount, total errors: $totalErrCount")
        return request.createResponseBuilder(HttpStatus.OK).body("SUCCESS: total runtime, total record count, and total error count reset")
            .build()
    }
}
