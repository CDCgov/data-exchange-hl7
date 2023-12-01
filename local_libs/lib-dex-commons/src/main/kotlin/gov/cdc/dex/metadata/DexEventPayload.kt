package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import java.util.*

data class DexEventPayload (
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("message_uuid") val messageUUID: String = id,
    @SerializedName("upload_id") val uploadID: String?,
    @SerializedName("destination_id") val destinationID: String?,
    @SerializedName("destination_event") val destinationEvent: String?,
    @SerializedName("message_info") val messageInfo: DexMessageInfo,
    val metadata: DexMetadata,
    @SerializedName("summary") val summary: SummaryInfo,
    @SerializedName("schema_version") val metadataVersion: String = "0.0.1",
    @SerializedName("schema_name") val schemaName: String = "DEX HL7v2",
    val content: String
)