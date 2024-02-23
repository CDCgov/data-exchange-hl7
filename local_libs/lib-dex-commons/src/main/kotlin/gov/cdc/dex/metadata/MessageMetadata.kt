package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName

class MessageMetadata(
    @SerializedName("message_uuid") val messageUUID: String,
    @SerializedName("single_or_batch") val singleOrBatch: String,
    @SerializedName("message_index") val messageIndex: Int =1,
    @SerializedName("message_hash") val messageHash: String,
    val content: String
)