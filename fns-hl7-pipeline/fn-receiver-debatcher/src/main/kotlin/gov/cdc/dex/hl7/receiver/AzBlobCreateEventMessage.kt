package gov.cdc.dex.hl7.receiver

import com.google.gson.annotations.SerializedName


data class AzBlobCreateEventMessage (
  @SerializedName("topic"           ) var topic           : String,
  @SerializedName("subject"         ) var subject         : String,
  @SerializedName("eventType"       ) var eventType       : String,
  @SerializedName("id"              ) var id              : String,
  @SerializedName("data"            ) var evHubData       : EvHubData,
  @SerializedName("dataVersion"     ) var dataVersion     : String,
  @SerializedName("metadataVersion" ) var metadataVersion : String,
  @SerializedName("eventTime"       ) var eventTime       : String

)