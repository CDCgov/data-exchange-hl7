package msc.edu
import com.azure.messaging.eventhubs.*
import org.junit.Test
import java.util.*


class EventHubSender {
    companion object {
        private val connString = "{{key here}}"
        private val eventHubName = "my-first-ktor"
    }

    @Test
    fun publishEvents() {
        // create a producer client
        val producer = EventHubClientBuilder()
            .connectionString(connString, eventHubName)
            .buildProducerClient()

        // sample events in an array
        val allEvents = Arrays.asList(EventData("Foo-3"), EventData("Bar-3"))

        // create a batch
        var eventDataBatch = producer.createBatch()

        for (eventData: EventData? in allEvents) {
            // try to add the event from the array to the batch
            if (!eventDataBatch.tryAdd(eventData)) {
                // if the batch is full, send it and then create a new batch
                producer.send(eventDataBatch)
                eventDataBatch = producer.createBatch()

                // Try to add that event that couldn't fit before.
                if (!eventDataBatch.tryAdd(eventData)) {
                    throw IllegalArgumentException(
                        "Event is too large for an empty batch. Max size: "
                                + eventDataBatch.maxSizeInBytes
                    )
                }
            }
        }
        // send the last batch of remaining events
        if (eventDataBatch.count > 0) {
            producer.send(eventDataBatch)
        }
        producer.close()
    }

}