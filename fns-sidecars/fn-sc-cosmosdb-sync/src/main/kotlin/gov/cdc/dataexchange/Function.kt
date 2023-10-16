package gov.cdc.dataexchange

import com.azure.cosmos.models.PartitionKey
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import gov.cdc.dataexchange.client.CosmosDBClient
import java.util.*
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dataexchange.client.CosmosDBClient.Companion.ConnectionFactory.closeConnectionShutdownHook
import gov.cdc.dataexchange.services.RecordService
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import java.lang.reflect.Type

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
