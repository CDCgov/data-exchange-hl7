package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName

data class ProfileConfiguration(
    @SerializedName("profile_identifiers") val profileIdentifiers : List<ProfileIdentifier>
)

data class ProfileIdentifier(
    val route : String,
    @SerializedName("identifier_paths") val identifierPaths: List<String>
)
