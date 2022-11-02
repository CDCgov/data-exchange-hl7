package gov.cdc.nist.validator

import java.io.InputStream

/**
 *
 *
 * @Created - 8/18/20
 * @Author Marcelo Caldas mcq1@cdc.gov
 */
class ResourceFileFetcher: ProfileFetcher {

    @Throws(InvalidFileException::class)
    override fun getFile(file: String): String {
        TODO("Not yet implemented")
    }

    @Throws(InvalidFileException::class)
    override fun getFileAsInputStream(file: String): InputStream {
        return getResourceFile(file)
    }


    private fun getResourceFile(fileName: String): InputStream {
        try {
            return ResourceFileFetcher::class.java.getResourceAsStream(fileName)
        } catch (e: NullPointerException) {
            throw InvalidFileException("Unable to load profile for $fileName")
        }
    }
}