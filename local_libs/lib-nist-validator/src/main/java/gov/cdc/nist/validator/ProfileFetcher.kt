package gov.cdc.nist.validator

import java.io.InputStream

/**
 *
 *
 * @Created - 8/18/20
 * @Author Marcelo Caldas mcq1@cdc.gov
 */
interface ProfileFetcher {
    @Throws(InvalidFileException::class)
    fun getFile(file: String): String
    @Throws(InvalidFileException::class)
    fun getFileAsInputStream(file: String): InputStream
}