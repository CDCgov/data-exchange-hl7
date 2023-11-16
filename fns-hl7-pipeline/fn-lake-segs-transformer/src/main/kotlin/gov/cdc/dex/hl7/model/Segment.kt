package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName

data class Segment (

  @SerializedName("segment") val segment: String, 
  @SerializedName("segment_number") val segmentNumber: Int, 
  @SerializedName("parent_segments") val parentSegments: List<String>?,

) { // .Profile
  @SerializedName("segment_id")
  var segmentId: String =
    if (segment.split('|')[0] != "root") {
      if (segment.split('|')[0] == "MSH") "MSH[1]"
      else "${segment.split('|')[0]}[${segment.split('|')[1]}]"
    } else "root"
}