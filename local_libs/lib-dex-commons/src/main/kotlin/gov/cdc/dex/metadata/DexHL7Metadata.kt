package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName

data class DexHL7Metadata(
    @SerializedName("schema_name") var schemaName: String = "DEX HL7v2",
    @SerializedName("schema_version") val schemaVersion: String = "2.0.0",
    @SerializedName("message_metadata") val messageMetadata: MessageMetadata,
    @SerializedName("routing_metadata") val routingInfo: RoutingMetadata,
    var stage: StageMetadata,
    var summary: SummaryInfo
)
