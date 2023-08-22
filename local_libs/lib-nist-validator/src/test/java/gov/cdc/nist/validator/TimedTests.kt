package gov.cdc.nist.validator

import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.system.measureTimeMillis

class TimedTests {

    @Test
    fun testValidateStructureErrors() {
        try {
            val nistValidator: ProfileManager
            val createTime = measureTimeMillis {
                nistValidator = ProfileManager(ResourceFileFetcher(), "/TEST_PROF")
            }
            println("Time to create nistValidator: $createTime")
            var nist: NistReport
            val testMessage = getTestFile("hl7TestMessage.txt")!!
            val time = measureTimeMillis {
                nist = nistValidator.validate(testMessage)
            }
            println("time to validate message: $time")

        } catch (e: Exception) {
            e.printStackTrace()
            assert(false)
        }
    }

    @Throws(IOException::class)
    private fun getTestFile(filename: String): String? {
        val classLoader = ClassLoader.getSystemClassLoader()
        val file = File(classLoader.getResource(filename).file)
        return String(Files.readAllBytes(file.toPath()))
    }

    @Test
    fun testLoadProfilesAsInputStream() {
        val profileFetcher = ResourceFileFetcher()
        val profile = "/COVID19_ELR-V2.3"

        val time = measureTimeMillis {
            val profileXML = profileFetcher.getFileAsInputStream("$profile/PROFILE.xml", true)
            val constraintsXML = profileFetcher.getFileAsInputStream("$profile/CONSTRAINTS.xml", false)
            val valueSetLibraryXML = profileFetcher.getFileAsInputStream("$profile/VALUESETS.xml", false)
            val valueSetBindingsXML = profileFetcher.getFileAsInputStream("$profile/VALUSETBINDINGS.xml", false)
            val slicingsXML = profileFetcher.getFileAsInputStream("$profile/SLICINGS.xml", false)
            val coConstraintsXML = profileFetcher.getFileAsInputStream("$profile/COCONSTRAINTS.xml", false)
        }
        println("millis to load profile streams: $time")
    }

    @Test
    fun testLoadProfilesAsString() {
        val profileFetcher = ResourceFileFetcher()
        val profile = "/COVID19_ELR-V2.3"
        var profileXML: String?
        val time = measureTimeMillis {
            profileXML = profileFetcher.getFile("$profile/PROFILE.xml", true)
            val constraintsXML = profileFetcher.getFile("$profile/CONSTRAINTS.xml", false)
            val valueSetLibraryXML = profileFetcher.getFile("$profile/VALUESETS.xml", false)
            val valueSetBindingsXML = profileFetcher.getFile("$profile/VALUSETBINDINGS.xml", false)
            val slicingsXML = profileFetcher.getFile("$profile/SLICINGS.xml", false)
            val coConstraintsXML = profileFetcher.getFile("$profile/COCONSTRAINTS.xml", false)
        }
        println("millis to load profile strings: $time")

    }
}