package gov.cdc.dex.hl7

import gov.cdc.dex.azure.EventHubSender

class FunctionConfig {
    companion object {
        val PROFILE_FILE_PATH = "PhinGuideProfile.json"
        val evHubConnStr = System.getenv("EventHubConnectionString")


    }


    val eventHubSendOkName = System.getenv("EventHubSendOkName")
    val eventHubSendErrsName = System.getenv("EventHubSendErrsName")

    val evHubSender = EventHubSender(evHubConnStr)
//    val profile = this::class.java.getResource("${/PROFILE_FILE_PATH}").readText()


}