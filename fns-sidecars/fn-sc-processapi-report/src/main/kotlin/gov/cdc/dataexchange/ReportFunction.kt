package gov.cdc.dataexchange

import com.azure.core.amqp.AmqpTransportType
import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dataexchange.model.ProcessingStatusSchema
import org.slf4j.LoggerFactory
import java.lang.System

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
        try {
            for(record in records) {
                val jsonObject = gson.fromJson(record, JsonObject::class.java)
                // form message for Processing Status API
                jsonObject.remove("content")
                val processingStatusSchema = ProcessingStatusSchema(content = jsonObject)
                val processingStatusJson = gson.toJson(processingStatusSchema)
                val messageUuid = jsonObject.get("message_uuid").asString
                logger.info("REPORT::upload_id: ${processingStatusSchema.uploadId}, message_uuid: $messageUuid, queue: $QUEUE" +
                        "\n$processingStatusJson")
                // send message to Processing Status API Service Bus
                serviceBusClient.sendMessage(ServiceBusMessage(processingStatusJson)).subscribe()
            }
        } catch (e: Exception) {
            logger.error("REPORT::ERROR: ${e.message}")
        }
    }
}
