package gov.cdc.dataexchange

import gov.cdc.dataexchange.client.CosmosDBClient
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dataexchange.client.CosmosDBClient.Companion.ConnectionFactory.closeConnectionShutdownHook
import gov.cdc.dataexchange.services.RecordService

/**
 * Azure Function implementations.
 * @Created - 10/11/2023
 * @Author QEH3@cdc.gov
 */
class Function {

    init {
        closeConnectionShutdownHook()
    }

    // Synchronize the incoming messages from EventHub to CosmosDB.
    @FunctionName("cosmos-sync")
    fun cosmosSync(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%"
        )
        records: List<String>
    ) {
        CosmosDBClient.bulkUpsert(RecordService.fluxRecords(records)).subscribe()
    }
}
