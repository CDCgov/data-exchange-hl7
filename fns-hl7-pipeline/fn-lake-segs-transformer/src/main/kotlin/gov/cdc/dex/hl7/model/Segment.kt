package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName

data class Segment (

  @SerializedName("segment") val segment: String,
  @SerializedName("segment_number") val segmentNumber: Int,
  @SerializedName("segment_id") val segmentId: String,
  @SerializedName("parent_segments") val parentSegments: List<String>?

) // .Profile