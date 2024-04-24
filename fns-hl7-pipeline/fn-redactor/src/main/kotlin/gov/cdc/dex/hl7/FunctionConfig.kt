package gov.cdc.dex.hl7

import com.azure.identity.DefaultAzureCredentialBuilder
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
        val evHubNamespace = System.getenv("EventHubConnection__fullyQualifiedNamespace")
        val entraClientId = System.getenv("EventHubConnection__clientId")
        val tokenCredential = DefaultAzureCredentialBuilder()
            .managedIdentityClientId(entraClientId)
            .build()
        evHubSender = DedicatedEventHubSender(evHubNamespace, evHubSendName, tokenCredential)

        val profileConfigJson = FunctionConfig::class.java.getResource("/$PROFILE_CONFIG_FILE_PATH")?.readText()
        profileConfig = JsonHelper.gson.fromJson(profileConfigJson, ProfileConfiguration::class.java)
    }
}