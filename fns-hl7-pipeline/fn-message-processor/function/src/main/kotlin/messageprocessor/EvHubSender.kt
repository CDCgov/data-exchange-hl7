package com.example

import com.azure.messaging.eventhubs.*
import com.google.gson.Gson 

class EvHubSender (
  val evHubName: String,
  val evHubConnStr: String,
) {
  fun send( hl7Message: HL7Message ) {

    val producer = EventHubClientBuilder()
        .connectionString(
            evHubConnStr,
            evHubName)
        .buildProducerClient()

    val hl7MessageJson = Gson().toJson(hl7Message)
    val hl7EventData = EventData(hl7MessageJson)

    val allEvents = arrayOf<EventData>(hl7EventData)

    var eventDataBatch = producer.createBatch()

    for ( eventData in allEvents) {
        // try to add the event from the array to the batch
        if (!eventDataBatch.tryAdd(eventData)) {
            // if the batch is full, send it and then create a new batch
            producer.send(eventDataBatch);
            eventDataBatch = producer.createBatch()

            // Try to add that event that couldn't fit before.
            if (!eventDataBatch.tryAdd(eventData)) {
                throw IllegalArgumentException("Event is too large for an empty batch. Max size: "
                    + eventDataBatch.getMaxSizeInBytes())
            } // .if 
        } // .if
    } // .for
    // send the last batch of remaining events
    if (eventDataBatch.getCount() > 0) {
        producer.send(eventDataBatch)
    }
    producer.close()

  } // .send
}