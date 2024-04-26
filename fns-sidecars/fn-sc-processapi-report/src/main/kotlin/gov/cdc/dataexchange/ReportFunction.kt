package gov.cdc.dataexchange

import com.azure.messaging.servicebus.ServiceBusMessage
import com.azure.messaging.servicebus.ServiceBusMessageBatch
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
            connection = "EventHubConnection",
            consumerGroup = "%EventHubConsumerGroup%",
            cardinality = Cardinality.MANY
        ) records: List<String>
    ) {
        val inputRecordCount = records.size
        logger.info("REPORT::Receiving $inputRecordCount records.")
        var batch = createNewBatch()
        for ((i, record) in records.withIndex()) {
            try {
                val processingStatusSchema = createProcessingStatusSchema(record)
                val processingStatusJson = JsonHelper.gson.toJson(processingStatusSchema)
                val sbMessage = ServiceBusMessage(processingStatusJson)
                // add message to batch
                if (!batch.tryAddMessage(sbMessage)) {
                    sendMessages(batch)
                    batch = createNewBatch()
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
            } catch (e: Exception) {
                logger.error("REPORT::Error: ${e.message}")
            }
        } //.for
        if (batch.count > 0) {
            try {
                sendMessages(batch)
            } catch (e: Exception) {
                logger.error("REPORT::ERROR sending batch to Service Bus queue ${fnConfig.sbQueue}: ${e.message}")
                //TODO: Unsure what else to do at this point? Send to Storage Account?
            }
        }
    }

    private fun sendMessages(batch: ServiceBusMessageBatch) {
        logger.info("REPORT::Sending batch of ${batch.count} messages")
        fnConfig.serviceBusSender.sendMessages(batch)
        logger.info("REPORT::Batch send completed")
    }

    private fun createNewBatch() : ServiceBusMessageBatch {
        return fnConfig.serviceBusSender.createMessageBatch(
            batchOptions.setMaximumSizeInBytes(fnConfig.maxMessageSize)
        )
    }

    private fun createProcessingStatusSchema(record: String): ProcessingStatusSchema {
        val inputEvent = JsonParser.parseString(record).asJsonObject
        inputEvent.remove("content")

        val uploadIdJson = JsonHelper.getValueFromJson( "routing_metadata.upload_id", inputEvent)
        val uploadId = if (uploadIdJson.isJsonNull) {
            UUID.randomUUID().toString()
        } else { uploadIdJson.asString }

        val destinationIdJson = JsonHelper.getValueFromJson( "routing_metadata.data_stream_id", inputEvent)
        val destinationId = if (destinationIdJson.isJsonNull) {
            "UNKNOWN"
        } else { destinationIdJson.asString }

        val eventTypeJson = JsonHelper.getValueFromJson( "routing_metadata.data_stream_route", inputEvent)
        val eventType = if (eventTypeJson.isJsonNull) {
            "UNKNOWN"
        } else { eventTypeJson.asString }

        // Extract the current stage
        val stageJson = JsonHelper.getValueFromJson("stage", inputEvent)
        val stage = if (!stageJson.isJsonNull) stageJson.asJsonObject else null

       val stageName = if (stage != null) {
            stage.remove("output")
            stage.get("stage_name").asString
        } else {
            "Unknown Stage"
        }

        // update schema_name to reflect this stage
        inputEvent.addProperty("schema_name", "DEX HL7v2 $stageName")

        return ProcessingStatusSchema(
            uploadId = uploadId,
            destinationId =  destinationId,
            eventType =  eventType,
            stageName =  stageName,
            content = inputEvent)
    }

}
