package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName

data class ConditionCode(

  @SerializedName("event_code") val eventCode: Long, 
  @SerializedName("name") val name: String, 
  @SerializedName("program") val program: String,
  @SerializedName("category") val category: String,
  @SerializedName("mmg_maps") val mmgMaps: Map<String, List<String>>,

) // .ConditionCode