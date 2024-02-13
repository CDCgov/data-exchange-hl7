package gov.cdc.dex.hl7

import com.google.gson.annotations.SerializedName
import gov.cdc.dex.metadata.DexMessageInfo
import gov.cdc.dex.metadata.DexMetadata
import gov.cdc.dex.metadata.RoutingMetadata
import gov.cdc.dex.metadata.SummaryInfo
import java.util.*

data class DexEventPayload2 (
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),   //required for CosmosDB
    @SerializedName("message_uuid") val messageUUID: String = id,
    @SerializedName("message_info") val messageInfo: DexMessageInfo2,
    @SerializedName("routing_metadata") val routingMetadata: RoutingMetadata,
    @SerializedName("metadata") val metadata: DexMetadata2,
    @SerializedName("summary") val summary: SummaryInfo,
    @SerializedName("schema_version") val schemaVersion: String = "0.0.1",
    @SerializedName("schema_name") val schemaName: String = "DEX HL7v2",
    val content: String
)