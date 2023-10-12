package gov.cdc.dataexchange

import gov.cdc.dataexchange.client.CosmosDBClient
import java.util.*
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import org.slf4j.LoggerFactory

/**
 * Azure Function implementations.
 * @Created - 10/11/2023
 * @Author QEH3@cdc.gov
 */
class Function {

    companion object {
        private var totalRecordCount = 0
        private var totalRuntime: Long = 0
        private val logger = LoggerFactory.getLogger(Function::class.java.simpleName)
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
        message: List<String>
    ) {
        val startTime = System.currentTimeMillis()
        CosmosDBClient.createAll(message)
        totalRecordCount += message.size
        totalRuntime += (System.currentTimeMillis() - startTime)
        logger.info("Sync'd ${message.size} messages in ${System.currentTimeMillis() - startTime}ms")
        logger.info("total records: $totalRecordCount in ${totalRuntime}ms")
    }

    @FunctionName("close-cosmos")
    fun closeCosmos(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET, HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>
    ): HttpResponseMessage {
        CosmosDBClient.Companion.ConnectionFactory.client.close()
        return request.createResponseBuilder(HttpStatus.OK).body("Cosmos DB Client closed successfully")
            .build()
    }
}
