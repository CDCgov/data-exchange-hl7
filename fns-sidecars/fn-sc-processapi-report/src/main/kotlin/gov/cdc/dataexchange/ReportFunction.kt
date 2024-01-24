package gov.cdc.dataexchange

import com.azure.core.amqp.AmqpTransportType
import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusMessage
import com.google.gson.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dataexchange.model.ProcessingStatusSchema
import org.slf4j.LoggerFactory
import java.lang.System
import java.util.*

/**
 * Azure Function implementations.
 * @Created - 11/28/2023
 * @Author QEH3@cdc.gov
 */
class ReportFunction {

    companion object {
        private val logger = LoggerFactory.getLogger(ReportFunction::class.java.simpleName)
        private val gson by lazy { Gson() }

        private val CONN_STR = System.getenv("ServiceBusConnectionString")
        private val QUEUE = System.getenv("ServiceBusQueue")

        private val serviceBusClient by lazy {
            ServiceBusClientBuilder()
                .connectionString(CONN_STR)
                .transportType(AmqpTransportType.AMQP_WEB_SOCKETS)
                .sender()
                .queueName(QUEUE)
                .buildAsyncClient()
        }
    }

    /**
     * Function to send service report to Processing Status API service bus
     * @param records
     */
    @FunctionName("processapi-report")
    fun processingStatusReport(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%"
        ) records: List<String>
    ) {
        val inputRecordCount = records.size
        logger.info("REPORT::Receiving $inputRecordCount records.")
        records.forEachIndexed { i, record ->
            try {
                val processingStatusSchema = createProcessingStatusSchema(record)
                val processingStatusJson = gson.toJson(processingStatusSchema)
                logger.info(
                    "REPORT::[${i + 1}] upload_id: ${processingStatusSchema.uploadId} sent to queue: $QUEUE"
                            //+
                            //"\n$processingStatusJson"
                )
                // send message to Processing Status API Service Bus asynchronously
                serviceBusClient.sendMessage(ServiceBusMessage(processingStatusJson)).subscribe(
                    {}, { e -> logger.error("Error sending message to Service Bus: ${e.message}\n" +
                            "upload_id: ${processingStatusSchema.uploadId}") }
                )
            } catch (e: JsonSyntaxException) {
                logger.error("REPORT::JSON Syntax Error: ${e.message}")
            } catch (e: IllegalStateException) {
                logger.error("REPORT::Illegal State Error: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("REPORT::General Error: ${e.message}")
            }
        }
    }

    private fun createProcessingStatusSchema(record: String): ProcessingStatusSchema {
        val content = gson.fromJson(record, JsonObject::class.java)
        content.remove("content")

        val uploadIdJson = extractValueFromPath(content, "upload_id")
        val uploadId = if (uploadIdJson == null || uploadIdJson.isJsonNull) {
            UUID.randomUUID().toString()
        } else { uploadIdJson.asString }

        val destinationIdJson = extractValueFromPath(content, "destination_id")
        val destinationId = if (destinationIdJson == null || destinationIdJson.isJsonNull) {
            "UNKNOWN"
        } else { destinationIdJson.asString }

        val eventTypeJson = extractValueFromPath(content, "destination_event")
        val eventType = if (eventTypeJson == null || eventTypeJson.isJsonNull) {
            "UNKNOWN"
        } else { eventTypeJson.asString }

        // Extract the last process from the processes array
        val lastProcess = extractValueFromPath(content, "metadata.processes")?.asJsonArray?.lastOrNull()
        val stageName = lastProcess?.asJsonObject?.get("process_name")?.asString ?: "Unknown Stage"

        return ProcessingStatusSchema(
            uploadId, destinationId, eventType, stageName, content = content)
    }

    private fun extractValueFromPath(jsonObject: JsonObject, path: String): JsonElement? {
        return try {
            val pathSegments: List<String> = path.split(".")
            var currentElement: JsonElement = jsonObject
            for (segment in pathSegments) {
                currentElement = currentElement.asJsonObject[segment] ?: return null
            }
            currentElement
        } catch (e: Exception) { null }
    }
}
