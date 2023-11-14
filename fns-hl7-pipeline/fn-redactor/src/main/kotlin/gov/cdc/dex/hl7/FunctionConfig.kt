package gov.cdc.dex.hl7

import gov.cdc.dex.azure.EventHubSender

class FunctionConfig {

    val evHubSender: EventHubSender
    val evHubSendName: String = System.getenv("EventHubSendName")

    init {
         //Init Event Hub connections
        val evHubConnStr = System.getenv("EventHubConnectionString")
        evHubSender = EventHubSender(evHubConnStr)


    }
}