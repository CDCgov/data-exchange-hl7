
import gov.cdc.nist.validator.InvalidFileException
import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.junit.jupiter.api.Test

class ProfileManagerTest {
    @Test
    fun testLoadProfiles() {
        try {
            val nistValidator = ProfileManager(ResourceFileFetcher(), "/profiles/NNDSS-NND_ORU_V2.0")
            println(nistValidator.profile)
        } catch (e: InvalidFileException) {
            throw RuntimeException(e)
        }
    }


}