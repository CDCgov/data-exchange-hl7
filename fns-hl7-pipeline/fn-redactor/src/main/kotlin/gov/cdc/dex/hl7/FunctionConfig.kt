package gov.cdc.dex.hl7

import gov.cdc.dex.azure.DedicatedEventHubSender

class FunctionConfig {
    val evHubSender: DedicatedEventHubSender
    val evHubSendName: String = System.getenv("EventHubSendName")
    
    init {
         //Init Event Hub connections
        val evHubConnStr = System.getenv("EventHubConnectionString")
        evHubSender = DedicatedEventHubSender(evHubConnStr, evHubSendName)
    }
}