package gov.cdc.dataexchange

import gov.cdc.dex.azure.cosmos.CosmosClient
import com.azure.cosmos.models.CosmosBulkOperationResponse
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dataexchange.services.RecordService
import org.slf4j.LoggerFactory
import reactor.util.function.Tuple2
import java.lang.System

/**
 * Azure Function implementations.
 * @Created - 11/28/2023
 * @Author QEH3@cdc.gov
 */
class ReportFunction {

    companion object {
        private val logger = LoggerFactory.getLogger(ReportFunction::class.java.simpleName)

        private const val CONN_STR = System.getenv("ServiceBusConnectionString")
        private const val QUEUE = System.getenv("ServiceBusQueue")

        private val client by lazy {
            ServiceBusClientBuilder()
                .connectionString(CONN_STR)
                .buildSenderClient(QUEUE)
        }
    }

    /**
     * Synchronize the incoming messages from EventHub by upserting to CosmosDB.
     * @param records records found in eventhub
     */
    @FunctionName("processapi-report")
    fun `processapi report`(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%"
        )
        records: List<String>
    ) {
        val inputRecordCount = records.size
        logger.info("REPORT::Receiving $inputRecordCount records.")
        try {
            for(record in records) {
                client.sendMessage(ServiceBusMessage(records))
            }
        } catch (e: Exception) {
            logger.error(e)
        } finally {
            client.close()
        }
    }
}
