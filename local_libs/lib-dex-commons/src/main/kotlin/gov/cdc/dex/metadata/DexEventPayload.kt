package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import java.util.*

data class DexEventPayload (
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("message_uuid") val messageUUID: String = id,
    @SerializedName("message_info") val messageInfo: DexMessageInfo,
    val metadata: DexMetadata,
    @SerializedName("summary") val summary: SummaryInfo,
    @SerializedName("metadata_version") val metadataVersion: String = "0.0.1",
    val content: String
)