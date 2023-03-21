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
    override fun getFile(file: String, req: Boolean): String? {
        TODO("Not yet implemented")
    }

    @Throws(InvalidFileException::class)
    override fun getFileAsInputStream(file: String, req: Boolean): InputStream? {
        return getResourceFile(file, req)
    }


    private fun getResourceFile(fileName: String, req:Boolean): InputStream? {
        try {
            return this::class.java.getResourceAsStream(fileName)!!
        } catch (e: NullPointerException) {
            if (req)
                throw InvalidFileException("Unable to load profile for $fileName")
            else return null
        }
    }
}