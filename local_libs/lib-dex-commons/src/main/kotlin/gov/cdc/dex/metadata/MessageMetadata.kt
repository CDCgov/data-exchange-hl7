package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import java.util.*

class MessageMetadata(
    @SerializedName("message_uuid") val messageUUID: String = UUID.randomUUID().toString(),
    @SerializedName("single_or_batch") val singleOrBatch: String,
    @SerializedName("message_index") val messageIndex: Int =1,
    @SerializedName("message_hash") val messageHash: String,

    ) {
    companion object {
        const val SINGLE_FILE = "SINGLE"
        const val BATCH_FILE = "BATCH"
    }
}