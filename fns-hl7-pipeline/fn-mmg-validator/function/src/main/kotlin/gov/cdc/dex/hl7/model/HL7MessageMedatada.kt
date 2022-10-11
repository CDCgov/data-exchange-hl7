package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName
data class HL7MessageMetadata (
  @SerializedName("file_path"          ) var filePath            : String,
  @SerializedName("file_name"          ) var fileName            : String,
  @SerializedName("file_timestamp"     ) var fileTimestamp       : String,
  @SerializedName("file_size"          ) var fileSize            : Long,
  @SerializedName("file_uuid"          ) var fileUUID            : String,
  @SerializedName("file_event_idD"     ) var fileEventID         : String,
  @SerializedName("file_event_timestamp") var fileEventTimestamp  : String,
  @SerializedName("message_uuid"       ) var messageUUID         : String? = null,
  @SerializedName("message_index"      ) var messageIndex        : Int? = null,
  
)