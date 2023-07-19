package gov.cdc.dex.hl7

import gov.cdc.dex.azure.EventHubSender

class FunctionConfig {
    companion object {
        const val PROFILE_FILE_PATH = "PhinGuideProfile.json"
        val evHubConnStr: String = System.getenv("EventHubConnectionString")
    }

    val eventHubSendOkName: String = System.getenv("EventHubSendOkName")
    val eventHubSendErrsName: String = System.getenv("EventHubSendErrsName")

    val evHubSender = EventHubSender(evHubConnStr)
//    val profile = this::class.java.getResource("${/PROFILE_FILE_PATH}").readText()


}