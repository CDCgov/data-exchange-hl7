package gov.cdc.dex.hl7
import gov.cdc.dex.util.PathUtils
import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import java.io.IOException
import java.nio.file.Files
import java.util.*
import kotlin.io.path.*

class FunctionConfig {

    val nistValidators = loadNistValidators()
    val functionVersion = System.getenv("FN_VERSION")?.toString() ?: "Unknown"

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