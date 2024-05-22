package gov.cdc.dex.hl7

import gov.cdc.dex.azure.DedicatedEventHubSender
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.ProfileConfiguration

class FunctionConfig {
    val PROFILE_CONFIG_FILE_PATH = "profiles/profile_config.json"
    val profileConfig : ProfileConfiguration
    val evHubSender: DedicatedEventHubSender
    val evHubSendName: String = System.getenv("EventHubSendName")
    
    init {
         //Init Event Hub connections
        val evHubConnStr = System.getenv("EventHubConnectionString")
        evHubSender = DedicatedEventHubSender(evHubConnStr, evHubSendName)

        val profileConfigJson = FunctionConfig::class.java.getResource("/$PROFILE_CONFIG_FILE_PATH")?.readText()
        profileConfig = JsonHelper.gson.fromJson(profileConfigJson, ProfileConfiguration::class.java)
    }
}