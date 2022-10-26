package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import java.util.*

data class Provenance(
    @SerializedName("file_path") val filePath: String,
    @SerializedName("file_timestamp") val fileTimestamp: String,
    @SerializedName("file_size") val fileSize: Long,
    @SerializedName("single_or_batch") val singleOrBatch: String,
    @SerializedName("message_hash") val messageHash: Int,
    @SerializedName("ext_system_provider") val systemProvider: String?,
    @SerializedName("ext_original_file_name") val originalFileName: String?,
    @SerializedName("message_index") val messageIndex: Int = 1,

) {
    companion object {
        const val SINGLE_FILE = "SINGLE"
        const val BATCH_FILE = "BATCH"
    }

    @SerializedName("file_uuid") val fileUUID:String  = UUID.randomUUID().toString()
}