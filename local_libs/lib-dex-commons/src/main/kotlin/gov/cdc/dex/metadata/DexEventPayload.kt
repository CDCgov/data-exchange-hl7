package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName
import java.util.*

data class DexEventPayload(
    @SerializedName("file_metadata") val dexFileMetadata: DEXFileMetadata,
    @SerializedName("hl7_metadata") val dexMetadata: DexMetadata,
    @SerializedName("summary") val summary: SummaryInfo,
    @SerializedName("schema_version") val schemaVersion: String = "0.0.1",
    @SerializedName("schema_name") val schemaName: String = "DEX HL7v2",
)