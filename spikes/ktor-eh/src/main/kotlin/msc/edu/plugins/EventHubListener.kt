package msc.edu.plugins

import com.azure.messaging.eventhubs.EventData
import com.azure.messaging.eventhubs.EventHubClientBuilder
import com.azure.messaging.eventhubs.EventProcessorClientBuilder
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore
import com.azure.messaging.eventhubs.models.ErrorContext
import com.azure.messaging.eventhubs.models.EventContext
import com.azure.messaging.eventhubs.models.PartitionContext
import com.azure.storage.blob.BlobContainerAsyncClient
import com.azure.storage.blob.BlobContainerClientBuilder
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.config.*


class EventHubConfiguration(config: ApplicationConfig) {
    var connectionString: String = config.tryGetString("connection_string") ?: ""
//    var namespaceName: String = config.tryGetString("namespace") ?: ""
    var eventHubName: String = config.tryGetString("eventhub_name") ?: "my-first-ktor"
    var storageConnectionString = config.tryGetString("storage_connection_string") ?: ""
    var storageContainerName = config.tryGetString("storage_container_name") ?: "event-hub-checkpt"
}

val EventHubListener = createApplicationPlugin(
    name = "EventHubListener",
    configurationPath = "azure.event_hub",
    createConfiguration = ::EventHubConfiguration) {

    // Create a blob container client that you use later to build an event processor client to receive and process events
    val blobContainerAsyncClient: BlobContainerAsyncClient = BlobContainerClientBuilder()
        .connectionString(pluginConfig.storageConnectionString)
        .containerName(pluginConfig.storageContainerName)
        .buildAsyncClient()

    // Create an event processor client to receive and process events and errors.


    val eventProcessorClient by lazy {
        EventProcessorClientBuilder()
            .connectionString(pluginConfig.connectionString)
            .eventHubName(pluginConfig.eventHubName)
            .consumerGroup(EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME)
            .processEvent { eventContext: EventContext -> processMessage(eventContext) }
            .processError { errorContext: ErrorContext -> processError(errorContext) }
            .checkpointStore(BlobCheckpointStore(blobContainerAsyncClient))
            .buildEventProcessorClient()
    }

    // handles received messages
    @Throws(InterruptedException::class)
    fun receiveMessages() {
        // Create an instance of the processor through the EventProcessorClient
        println("Starting the Azure eventhub  processor")
        println("connectionString = $pluginConfig.connectionString, eventHub: = ${pluginConfig.eventHubName}")
        eventProcessorClient.start()
    }

    on(MonitoringEvent(ApplicationStarted)) { application ->
        application.log.info("Server is started")
        receiveMessages()
    }
    on(MonitoringEvent(ApplicationStopped)) { application ->
        application.log.info("Server is stopped")
        println("Stopping and closing the processor")
        eventProcessorClient.stop()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStarted) {}
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }
}
    fun processError(errorContext: ErrorContext) {
        System.out.printf(
            "Error occurred in partition processor for partition {}, {}",
            errorContext.partitionContext.partitionId,
            errorContext.throwable)

    }

    fun processMessage(context: EventContext) {
        val partitionContext: PartitionContext = context.partitionContext
        val eventData: EventData = context.eventData

        System.out.printf(
            "Processing event from partition %s with sequence number %d with body: %s%n",
            partitionContext.partitionId, eventData.sequenceNumber, eventData.bodyAsString
        )

        // Every 10 events received, it will update the checkpoint stored in Azure Blob Storage.
        if (eventData.sequenceNumber % 10 == 0L) {
            context.updateCheckpoint()
        }
    }
//}


fun Application.eventHubListenerModule() {
    install(EventHubListener) {
        // any additional configuration goes here
    }
}
