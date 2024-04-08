package gov.cdc.dex.azure

import com.azure.core.amqp.exception.AmqpException
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

    fun getPartitionIds() : List<String> {
        return producer.partitionIds.map { it.toString() }
    }

    fun send(message: String) : List<Int> {
        return send(listOf(message))
    }

    fun send(messages: List<String>) : List<Int> {
        var eventDataBatch = producer.createBatch()
        // if any error out due to size, note which ones.
        // any other errors will propagate back to the caller
        val errors: MutableList<Int> = mutableListOf()
        messages.forEachIndexed { index,  msg ->
            // try to add the event from the array to the batch
            val eventData = EventData(msg)
            if (eventData.body.size > eventDataBatch.maxSizeInBytes) {
                errors.add(index)
            } else {
                if (!eventDataBatch.tryAdd(eventData)) {
                    // if the batch has data, send it and then create a new batch
                    if (eventDataBatch.count > 0) {
                        producer.send(eventDataBatch)
                        eventDataBatch = producer.createBatch()

                        // Try to add that event that couldn't fit before.
                        try {
                            if (!eventDataBatch.tryAdd(eventData)) {
                                // Event is too large for empty batch
                                errors.add(index)
                            } // .if
                        } catch (e: AmqpException) {
                            // tryAdd will (sometimes?) throw an AmqpException if the
                            // event data exceeds the max batch size.
                            errors.add(index)
                        }
                    } else {
                        // Event is too large for empty batch
                        errors.add(index)
                    }
                } // .if
            }
        } // .for
        // send the last batch of remaining events
        if (eventDataBatch.count > 0) {
            producer.send(eventDataBatch)
        }
        return errors
    } // .send


}