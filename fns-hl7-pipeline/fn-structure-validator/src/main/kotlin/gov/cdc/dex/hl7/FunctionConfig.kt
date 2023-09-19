package gov.cdc.dex.hl7
import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.slf4j.LoggerFactory

class FunctionConfig {

    private val nistValidators = mutableMapOf<String, ProfileManager?>()
    val functionVersion = System.getenv("FN_VERSION")?.toString() ?: "Unknown"
    private var logger = LoggerFactory.getLogger(FunctionConfig::class.java.simpleName)

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