package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName

data class RoutingMetadata(
    @SerializedName("upload_id") val uploadID: String?,
    @SerializedName("trace_id") val traceID: String?,
    @SerializedName("parent_span_id") val parentSpanID: String?,
    @SerializedName("destination_id") val destinationID: String?,
    @SerializedName("destination_event") val destinationEvent: String?
)
