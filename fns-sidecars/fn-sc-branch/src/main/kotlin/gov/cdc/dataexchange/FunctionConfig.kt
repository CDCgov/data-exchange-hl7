package gov.cdc.dataexchange

import gov.cdc.dex.azure.EventHubSender

class FunctionConfig {
    val evHubSendOkName: String = System.getenv("EventHubSendOkName")
    val evHubSendErrsName: String = System.getenv("EventHubSendErrsName")
    val evHubSender : EventHubSender

    init {
        //Init Event Hub connections
        val evHubConnStr = System.getenv("EventHubConnectionString")
        evHubSender = EventHubSender(evHubConnStr)
    }
}