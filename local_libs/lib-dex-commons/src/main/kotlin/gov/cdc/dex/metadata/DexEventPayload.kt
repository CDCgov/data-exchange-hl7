package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import java.util.*

data class DexEventPayload (
    val content: String,
    val metadata: DexMetadata,
    @SerializedName("summary") val summary: SummaryInfo,
    @SerializedName("message_uuid") val messageUUID: String = UUID.randomUUID().toString(),
    val metadataVersion: String = "1.0.0"
)