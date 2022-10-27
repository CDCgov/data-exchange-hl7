package gov.cdc.dex.azure

import com.azure.messaging.eventhubs.*

class EventHubSender (val evHubConnStr: String, ) {
    fun send(  evHubTopicName: String, message: String ) {
        val producer = EventHubClientBuilder()
            .connectionString(evHubConnStr,  evHubTopicName)
            .buildProducerClient()

        val hl7EventData = EventData(message)
        val allEvents = arrayOf<EventData>(hl7EventData)

        var eventDataBatch = producer.createBatch()
        allEvents.forEach {eventData ->
            // try to add the event from the array to the batch
            if (!eventDataBatch.tryAdd(eventData)) {
                // if the batch is full, send it and then create a new batch
                producer.send(eventDataBatch)
                eventDataBatch = producer.createBatch()

                // Try to add that event that couldn't fit before.
                if (!eventDataBatch.tryAdd(eventData)) {
                    throw IllegalArgumentException("Event is too large for an empty batch. Max size: "
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