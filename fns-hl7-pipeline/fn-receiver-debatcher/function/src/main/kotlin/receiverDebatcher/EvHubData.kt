package com.example

import com.google.gson.annotations.SerializedName


data class EvHubData (

  @SerializedName("api"                ) var api                     : String,
  @SerializedName("clientRequestId"    ) var clientRequestId         : String,
  @SerializedName("requestId"          ) var requestId               : String,
  @SerializedName("eTag"               ) var eTag                    : String,
  @SerializedName("contentType"        ) var contentType             : String,
  @SerializedName("contentLength"      ) var contentLength           : Int,
  @SerializedName("blobType"           ) var blobType                : String,
  @SerializedName("url"                ) var url                     : String,
  @SerializedName("sequencer"          ) var sequencer               : String,
  @SerializedName("storageDiagnostics" ) var evHubstorageDiagnostics : EvHubStorageDiagnostics

)