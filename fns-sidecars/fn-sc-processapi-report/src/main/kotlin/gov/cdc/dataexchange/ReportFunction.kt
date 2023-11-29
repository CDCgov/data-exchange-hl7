package gov.cdc.dataexchange

import com.azure.core.amqp.AmqpTransportType
import com.azure.messaging.servicebus.ServiceBusClientBuilder
import com.azure.messaging.servicebus.ServiceBusMessage
import com.microsoft.azure.functions.annotation.*
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
                serviceBusClient.sendMessage(ServiceBusMessage(record)).subscribe()
            }
        } catch (e: Exception) {
            logger.error("REPORT::ERROR: ${e.message}")
        }
    }
}
