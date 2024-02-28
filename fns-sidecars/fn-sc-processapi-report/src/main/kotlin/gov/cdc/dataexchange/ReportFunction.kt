package gov.cdc.dataexchange

import com.azure.messaging.servicebus.ServiceBusMessage
import com.azure.messaging.servicebus.models.CreateMessageBatchOptions
import com.google.gson.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dataexchange.model.ProcessingStatusSchema
import gov.cdc.dex.util.JsonHelper
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Azure Function implementations.
 * @Created - 11/28/2023
 * @Author QEH3@cdc.gov
 */
class ReportFunction {

    companion object {
        private val logger = LoggerFactory.getLogger(ReportFunction::class.java.simpleName)
        val fnConfig = FunctionConfig()
        val batchOptions = CreateMessageBatchOptions()
        val MAX_MESSAGE_SIZE = 262144
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
            consumerGroup = "%EventHubConsumerGroup%",
            cardinality = Cardinality.MANY
        ) records: List<String>
    ) {
        val inputRecordCount = records.size
        logger.info("REPORT::Receiving $inputRecordCount records.")
        var batch = fnConfig.serviceBusSender.createMessageBatch(
            batchOptions.setMaximumSizeInBytes(MAX_MESSAGE_SIZE)
        )

        for ((i, record) in records.withIndex()) {
            try {
                val processingStatusSchema = createProcessingStatusSchema(record)
                val processingStatusJson = JsonHelper.gson.toJson(processingStatusSchema)
                val sbMessage = ServiceBusMessage(processingStatusJson)
                // add message to batch
                if (!batch.tryAddMessage(sbMessage)) {
                    fnConfig.serviceBusSender.sendMessages(batch)
                    logger.info("REPORT::Sending batch of ${batch.count} messages")
                    batch = fnConfig.serviceBusSender.createMessageBatch(
                        batchOptions.setMaximumSizeInBytes(MAX_MESSAGE_SIZE)
                    )
                    logger.info("REPORT::Batch send completed")
                    // add the message we could not add before
                    if (!batch.tryAddMessage(sbMessage)) {
                        logger.error("REPORT::[${i + 1}] MESSAGE TOO LARGE ERROR: upload_id: ${processingStatusSchema.uploadId}")
                    } else {
                        logger.info("REPORT::[${i + 1}] upload_id: ${processingStatusSchema.uploadId} added to batch")
                    }
                }  else {
                    logger.info("REPORT::[${i + 1}] upload_id: ${processingStatusSchema.uploadId} added to batch")
                }//.if
            } catch (e: JsonSyntaxException) {
                logger.error("REPORT::JSON Syntax Error: ${e.message}")
            } catch (e: IllegalStateException) {
                logger.error("REPORT::Illegal State Error: ${e.message}")
            } catch (e: Exception) {
                e.printStackTrace()
                logger.error("REPORT::General Error: ${e.message}")
            }
        } //.for
        if (batch.count > 0) {
            try {
                logger.info("REPORT::Sending batch of ${batch.count} messages")
                fnConfig.serviceBusSender.sendMessages(batch)
                logger.info("REPORT::Batch send completed")
            } catch (e: Exception) {
                logger.error("REPORT::ERROR sending batch to Service Bus queue ${fnConfig.sbQueue}: ${e.message}")
                //TODO: Unsure what else to do at this point? Send to Storage Account?
            }
        }
    }

    private fun createProcessingStatusSchema(record: String): ProcessingStatusSchema {
        val content = JsonHelper.gson.fromJson(record, JsonObject::class.java)
        content.remove("content")

        val uploadIdJson = extractValueFromPath(content, "routing_metadata.upload_id")
        val uploadId = if (uploadIdJson == null || uploadIdJson.isJsonNull) {
            UUID.randomUUID().toString()
        } else { uploadIdJson.asString }

        val destinationIdJson = extractValueFromPath(content, "routing_metadata.data_stream_id")
        val destinationId = if (destinationIdJson == null || destinationIdJson.isJsonNull) {
            "UNKNOWN"
        } else { destinationIdJson.asString }

        val eventTypeJson = extractValueFromPath(content, "routing_metadata.data_stream_route")
        val eventType = if (eventTypeJson == null || eventTypeJson.isJsonNull) {
            "UNKNOWN"
        } else { eventTypeJson.asString }

        // Extract the current stage
        val stage = extractValueFromPath(content, "metadata.stage")?.asJsonObject

       val stageName = if (!(stage == null || stage.isJsonNull)) {
            stage.remove("output")
            stage.get("stage_name").asString
        } else {
            "Unknown Stage"
        }

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
