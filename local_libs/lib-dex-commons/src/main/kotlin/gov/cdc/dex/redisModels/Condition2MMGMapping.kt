package gov.cdc.dex.redisModels

import com.google.gson.annotations.SerializedName

data class Condition2MMGMapping(
  @SerializedName("event_code") val eventCode: Long,
  @SerializedName("name") val name: String,
  @SerializedName("program") val program: String,
  @SerializedName("category") val category: String,
  @SerializedName("profiles") val profiles: List<Profile>?
  //@SerializedName("type") val type: MessageType,

  ) // .ConditionCode

data class SpecialCase(
  @SerializedName("applies_to") val appliesTo: String,
  @SerializedName("mmgs") val mmgs: List<String>,
) // .SpecialCase

data class Profile(
  @SerializedName("name") val name: String,
  @SerializedName("mmgs") val mmgs: List<String>,
  @SerializedName("special_cases") val specialCases: List<SpecialCase>?,

  ) // .Profile

