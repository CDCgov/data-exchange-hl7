package com.example

import com.google.gson.annotations.SerializedName


data class HL7Message (

  @SerializedName("content"  ) var content  : String,
  @SerializedName("metadata" ) var metadata : HL7MessageMetadata,

)