package gov.cdc.dex.azure

import com.azure.messaging.eventhubs.EventData
import com.azure.messaging.eventhubs.EventHubClientBuilder
import com.azure.messaging.eventhubs.EventHubProducerClient

class DedicatedEventHubSender( evHubConnStr: String, evHubTopicName: String) {
    private val producer: EventHubProducerClient = EventHubClientBuilder()
        .connectionString(evHubConnStr, evHubTopicName)
        .buildProducerClient()

    fun disconnect() {
        producer.close()
    }

    fun send(message: String) {
        send(listOf(message))
    }

    fun send(messages: List<String>) {

        var eventDataBatch = producer.createBatch()
        messages.forEach { msg ->
            // try to add the event from the array to the batch
            if (!eventDataBatch.tryAdd(EventData(msg))) {
                // if the batch is full, send it and then create a new batch
                producer.send(eventDataBatch)
                eventDataBatch = producer.createBatch()

                // Try to add that event that couldn't fit before.
                if (!eventDataBatch.tryAdd(EventData(msg))) {
                    throw IllegalArgumentException(
                        "Event is too large for an empty batch. Max size: "
                                + eventDataBatch.maxSizeInBytes
                    )
                } // .if
            } // .if
        } // .for
        // send the last batch of remaining events
        if (eventDataBatch.count > 0) {
            producer.send(eventDataBatch)
        }
    } // .send
}