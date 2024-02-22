package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName

class MessageMetadata(
    @SerializedName("single_or_batch") val singleOrBatch: String,
    @SerializedName("message_index") val messageIndex: Int =1,
    @SerializedName("message_hash") val messageHash: String,
    @SerializedName("ingested_file_path") val ingestedFilePath: String,
    val content: String,
    @SerializedName("message_id") val messageUUID: String
)