package gov.cdc.dex.hl7

import gov.cdc.dex.azure.EventHubSender

class FunctionConfig {
    companion object {
        const val PROFILE_FILE_PATH = "PhinGuideProfile.json"
    }
    val evHubSendName: String = System.getenv("EventHubSendName")
    val evHubSender : EventHubSender

    init {
        //Init Event Hub connections
        val evHubConnStr = System.getenv("EventHubConnectionString")
        evHubSender = EventHubSender(evHubConnStr)
    }
}