package gov.cdc.dex.hl7

import com.azure.messaging.eventhubs.*


class EvHubSender(val evHubName: String, val evHubConnStr: String) {
    private val producer: EventHubProducerClient = EventHubClientBuilder()
        .connectionString(
            evHubConnStr,
            evHubName
        )
        .buildProducerClient()

    fun send(message: String) {

        val messages = EventData(message)

        val allEvents = arrayOf(messages)

        var eventDataBatch = producer.createBatch()

        for (eventData in allEvents) {
            // try to add the event from the array to the batch
            if (!eventDataBatch.tryAdd(eventData)) {
                // if the batch is full, send it and then create a new batch
                producer.send(eventDataBatch);
                eventDataBatch = producer.createBatch()

                // Try to add that event that couldn't fit before.
                if (!eventDataBatch.tryAdd(eventData)) {
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
        producer.close()

    } // .send
}