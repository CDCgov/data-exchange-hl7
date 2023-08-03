package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName

data class PhinDataType(

  @SerializedName("fieldNumber")  val fieldNumber: Long, 
  @SerializedName("name")         val name: String, 
  @SerializedName("dataType")     val dataType: String,
  @SerializedName("maxLength")    val maxLength: String,
  @SerializedName("usage")        val usage: String,
  @SerializedName("cardinality")  val cardinality: String,
  @SerializedName("conformance")  val conformance: String,
  @SerializedName("notes")        val notes: String,
  @SerializedName("preferred")    val preferred: Boolean,

) // .PhinDataType
