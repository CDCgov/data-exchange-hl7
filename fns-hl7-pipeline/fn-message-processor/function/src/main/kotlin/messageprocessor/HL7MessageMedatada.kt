package com.example

import com.google.gson.annotations.SerializedName


data class HL7MessageMetadata (

  @SerializedName("filePath"          ) var filePath            : String,
  @SerializedName("fileName"          ) var fileName            : String,
  @SerializedName("fileTimestamp"     ) var fileTimestamp       : String,
  @SerializedName("fileSize"          ) var fileSize            : Long,
  @SerializedName("fileUUID"          ) var fileUUID            : String,
  @SerializedName("fileEventID"       ) var fileEventID         : String,
  @SerializedName("fileEventTimestamp") var fileEventTimestamp  : String,
  @SerializedName("messageUUID"       ) var messageUUID         : String? = null,
  @SerializedName("messageIndex"      ) var messageIndex        : Int? = null,
  
)