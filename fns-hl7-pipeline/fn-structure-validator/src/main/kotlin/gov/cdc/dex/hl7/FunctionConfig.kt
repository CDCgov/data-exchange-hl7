package gov.cdc.dex.hl7
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.nist.validator.ProfileManager
import gov.cdc.dex.util.PathUtils
import gov.cdc.nist.validator.ResourceFileFetcher
import java.nio.file.Files
import kotlin.io.path.*

class FunctionConfig {
    companion object {
        val evHubConnStr: String = System.getenv("EventHubConnectionString")
    }

    val eventHubSendOkName: String = System.getenv("EventHubSendOkName")
    val eventHubSendErrsName: String = System.getenv("EventHubSendErrsName")
    val evHubSender = EventHubSender(evHubConnStr)
    val nistValidators = loadNistValidators()

    private fun loadNistValidators() : Map<String, ProfileManager> {
        val validatorMap = mutableMapOf<String, ProfileManager>()
        val dir = PathUtils().getResourcePath("profiles")
        println("Directory: ${dir.fileName}")
        Files.walk(dir).filter { it.isDirectory() && it.fileName != dir.fileName }.forEach { d ->
            val validator = ProfileManager(ResourceFileFetcher(), "/profiles/${d.fileName}")
            validatorMap["${d.fileName}"] = validator
        }
        return validatorMap.toMap()
    }

}