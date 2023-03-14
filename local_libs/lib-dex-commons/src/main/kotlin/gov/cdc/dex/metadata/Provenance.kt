package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import java.util.*

data class Provenance(
    @SerializedName("event_id") val eventId: String,
    @SerializedName("event_timestamp") val eventTimestamp: String,
    @SerializedName("file_uuid") val fileUUID:String  = UUID.randomUUID().toString(),
    @SerializedName("file_path") val filePath: String,
    @SerializedName("file_timestamp") val fileTimestamp: String,
    @SerializedName("file_size") val fileSize: Long,
    @SerializedName("single_or_batch") var singleOrBatch: String,
    @SerializedName("message_hash") var messageHash: String = "",
    @SerializedName("ext_system_provider") val systemProvider: String?,
    @SerializedName("ext_original_file_name") val originalFileName: String?,
    @SerializedName("message_index") var messageIndex: Int = 1,
    @SerializedName("ext_original_file_timestamp") val originalFileTimestamp: String?,
) {
    companion object {
        const val SINGLE_FILE = "SINGLE"
        const val BATCH_FILE = "BATCH"
    }

   // @SerializedName("file_uuid") val fileUUID:String  = UUID.randomUUID().toString()
}