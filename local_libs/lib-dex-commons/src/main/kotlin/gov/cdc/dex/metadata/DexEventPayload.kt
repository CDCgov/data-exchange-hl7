package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import java.util.*

data class DexEventPayload (
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),   //required for CosmosDB
    @SerializedName("message_uuid") val messageUUID: String = id,
    @SerializedName("message_info") val messageInfo: DexMessageInfo,
    @SerializedName("routing_metadata") val routingMetadata: RoutingMetadata,
    @SerializedName("metadata") val metadata: DexMetadata,
    @SerializedName("summary") val summary: SummaryInfo,
    @SerializedName("schema_version") val schemaVersion: String = "0.0.1",
    @SerializedName("schema_name") val schemaName: String = "DEX HL7v2",
    val content: String
)