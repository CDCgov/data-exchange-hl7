package gov.cdc.dataexchange.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import gov.cdc.dex.metadata.DexEventPayload
import java.util.*

/**
 * Defines JSON Schema for Processing Status Api Service Bus
 * @param uploadId for "upload_id" field
 * @param destinationId for "destination_id" field
 * @param eventType for "event_type" field
 * @param stageName for "stage_name" field
 * @param contentType for "content_type" field
 * @param content for "content" field
 */
data class ProcessingStatusSchema(

    @SerializedName("upload_id")
    val uploadId: String?,

    @SerializedName("destination_id")
    val destinationId: String?,

    @SerializedName("event_type")
    val eventType: String?,

    @SerializedName("stage_name")
    val stageName: String?,

    @SerializedName("content_type")
    val contentType: String = "json",

    @SerializedName("content")
    var content: JsonObject
)