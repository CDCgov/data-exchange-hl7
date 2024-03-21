package gov.cdc.dex.hl7

import gov.cdc.dex.azure.DedicatedEventHubSender

class FunctionConfig {
    companion object {
        const val PROFILE_FILE_PATH = "PhinGuideProfile_v2.json"
    }
    val evHubSendName: String = System.getenv("EventHubSendName")
    val evHubSender : DedicatedEventHubSender
    val psURL = System.getenv("ProcessingStatusBaseURL")

    init {
        //Init Event Hub connections
        val evHubConnStr = System.getenv("EventHubConnectionString")
        evHubSender = DedicatedEventHubSender(evHubConnStr, evHubSendName)
    }
}