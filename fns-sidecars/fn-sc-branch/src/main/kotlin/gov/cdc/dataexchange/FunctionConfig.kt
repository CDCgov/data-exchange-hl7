package gov.cdc.dataexchange

import gov.cdc.dex.azure.DedicatedEventHubSender

class FunctionConfig {
    val evHubSendOkName: String = System.getenv("EventHubSendOkName")
    val evHubSendErrsName: String = System.getenv("EventHubSendErrsName")
    val evHubSenderOk : DedicatedEventHubSender
    val evHubSenderErr : DedicatedEventHubSender

    init {
        //Init Event Hub connections
        val evHubConnStr = System.getenv("EventHubConnectionString")
        evHubSenderOk = DedicatedEventHubSender(evHubConnStr, evHubSendOkName)
        evHubSenderErr = DedicatedEventHubSender(evHubConnStr, evHubSendErrsName)
    }
}