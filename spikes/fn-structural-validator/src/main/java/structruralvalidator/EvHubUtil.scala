package cdc.ede.hl7.structuralvalidator

import com.azure.messaging.eventhubs._


object EvHubUtil {

  def evHubSend(evHubConnStr: String, evHubName: String, message: String ) = {

      val producer = new EventHubClientBuilder()
          .connectionString(evHubConnStr,evHubName)
          .buildProducerClient()

      val hl7EventData = new EventData(message)

      val allEvents = List(hl7EventData)

      var eventDataBatch = producer.createBatch()

      for ( eventData <- allEvents) {
          // try to add the event from the array to the batch
          if (!eventDataBatch.tryAdd(eventData)) {
              // if the batch is full, send it and then create a new batch
              producer.send(eventDataBatch);
              eventDataBatch = producer.createBatch()

              // Try to add that event that couldn't fit before.
              if (!eventDataBatch.tryAdd(eventData)) {
                  throw new IllegalArgumentException("Event is too large for an empty batch. Max size: "
                      + eventDataBatch.getMaxSizeInBytes())
              } // .if 
          } // .if
      } // .for

      // send the last batch of remaining events
      if (eventDataBatch.getCount() > 0) {
          producer.send(eventDataBatch)
      } // .if
      producer.close()

  } // .evHubSend


} // .EvHubUtil


