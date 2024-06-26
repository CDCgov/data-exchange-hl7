package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName

class RoutingMetadata (
    @SerializedName("dex_ingest_datetime") val dexIngestDateTime: String,
    @SerializedName("ingested_file_path") val ingestedFilePath: String,
    @SerializedName("ingested_file_timestamp") val ingestedFileTimestamp: String,
    @SerializedName("ingested_file_size") val ingestedFileSize: String,
    @SerializedName("data_producer_id") val dataProducerId: String,
    @SerializedName("jurisdiction") val jurisdiction: String,
    @SerializedName("upload_id") val uploadId: String,
    @SerializedName("data_stream_id") val dataStreamId: String,
    @SerializedName("data_stream_route") val dataStreamRoute: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("received_filename") val receivedFilename: String,
    @SerializedName("supporting_metadata") var supportingMetadata: Map<String, String>? = null,
    @SerializedName("version") var version: String? = null
)