package gov.cdc.dex.hl7
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.hl7.model.ProfileConfiguration
import gov.cdc.dex.util.JsonHelper.gson
import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.slf4j.LoggerFactory

class FunctionConfig {
    companion object {
        const val PROFILE_CONFIG_FILE_PATH = "profiles/profile_config.json"
    }
    private val nistValidators = mutableMapOf<String, ProfileManager?>()
    val functionVersion = System.getenv("FN_VERSION")?.toString() ?: "Unknown"
    private var logger = LoggerFactory.getLogger(FunctionConfig::class.java.simpleName)
    val evHubSendName: String = System.getenv("EventHubSendName")
    val evHubSender : EventHubSender
    val profileConfig : ProfileConfiguration

    init {
        //Init Event Hub connections
        val evHubConnStr = System.getenv("EventHubConnectionString")
        evHubSender = EventHubSender(evHubConnStr)
        val profileConfigJson = FunctionConfig::class.java.getResource("/$PROFILE_CONFIG_FILE_PATH")?.readText()
        profileConfig = gson.fromJson(profileConfigJson, ProfileConfiguration::class.java)
    }
    fun getNistValidator(profileName: String) : ProfileManager? {
        if (nistValidators[profileName] == null) {
            loadNistValidator(profileName)
        }
        return nistValidators[profileName]
    }
    private fun loadNistValidator(profileName : String)  {
        val validator = try {
            ProfileManager(ResourceFileFetcher(), "/profiles/$profileName")
        } catch (e : Exception) {
            logger.error("${e.message}")
            null
        }
        nistValidators[profileName] = validator
    }

}