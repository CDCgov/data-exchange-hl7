package gov.cdc.ncezid.cloud.storage

import gov.cdc.ncezid.cloud.ProviderMeta
import io.micronaut.http.MediaType
import java.io.InputStream

// TODO - consider splitting interface, one with default bucket and one without


const val META_CONTENT_LENGTH = "content_length"
const val META_CONTENT_TYPE = "content_type"
const val META_CONTENT_ENCODING = "content_encoding"

/**
 *
 *
 * @Created - 9/18/20
 * @Author Marcelo Caldas mcq1@cdc.gov
 */
interface CloudStorage : ProviderMeta {
    fun list(bucket: String, maxNumber: Int, prefix: String? = null): List<String>
    fun list(maxNumber: Int, prefix: String? = null): List<String>
    fun listFolders(bucket: String): List<String>
    fun listFolders(): List<String>
    fun getFile(fileName: String): CloudFile
    fun getFile(bucket: String, fileName: String): CloudFile =
        CloudFile(bucket, fileName, getMetadata(bucket, fileName), getFileContent(bucket, fileName))
    fun getFileContent(bucket: String, fileName: String): String
    fun getFileContent(fileName: String): String
    fun getFileContentAsInputStream(bucket: String, fileName: String): InputStream
    fun getFileContentAsInputStream(fileName: String): InputStream
    fun getMetadata(bucket: String, fileName: String, urlDecode: Boolean = true): Map<String, String>
    fun getMetadata(fileName: String, urlDecode: Boolean = true): Map<String, String>
    fun getDefaultBucket(): String

    fun saveFile(bucket: String, fileName: String, content:String, metadata: Map<String, String>? = null, contentType: String = MediaType.TEXT_PLAIN)
    fun saveFile(bucket: String, fileName: String, content:InputStream, size: Long, metadata: Map<String, String>? = null, contentType: String = MediaType.TEXT_PLAIN)
    fun saveFile(fileName: String, content:String, metadata: Map<String, String>? = null, contentType: String = MediaType.TEXT_PLAIN)
    fun saveFile(fileName: String, content:InputStream, size: Long, metadata: Map<String, String>? = null, contentType: String = MediaType.TEXT_PLAIN)
    fun deleteFile(bucket:String, fileName: String): Int
    fun deleteFile(fileName: String): Int

    fun healthCheck(): String
}

