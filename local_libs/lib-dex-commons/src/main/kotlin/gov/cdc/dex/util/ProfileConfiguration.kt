package gov.cdc.dex.util

import com.google.gson.annotations.SerializedName
/* 'identifier paths' tells us where to look
 in the HL7 message for the profile identifiers
 for different data streams.
  e.g., for data_stream_id 'PHLIP', identifier path is 'MSH-21[2].1'*/

data class ProfileConfiguration(
    @SerializedName("profile_identifiers") val profileIdentifiers : List<ProfileIdentifier>
)

data class ProfileIdentifier(
    @SerializedName("data_stream_id") val dataStreamId : String,
    @SerializedName("identifier_paths") val identifierPaths: List<String>
)