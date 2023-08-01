package gov.cdc.dex.hl7.worker

import com.azure.messaging.eventhubs.*
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.*
import org.slf4j.LoggerFactory
import java.io.*
import java.util.*


class Function {

    companion object {
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    }

    @FunctionName("cosmos-worker")
    fun worker(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            consumerGroup = "%EventHubConsumerGroup%",
            connection = "EventHubConnectionString")
        messages: List<String>?,
        @BindingName("SystemPropertiesArray") eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext): String? {

        logger.info("Triggering Cosmos Worker")

        return "Test"
    }
}