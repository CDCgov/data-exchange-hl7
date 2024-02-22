package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName

data class DexMetadata(
    @SerializedName("message_metadata") val messageMetadata: MessageMetadata,
    var stage: StageMetadata
)
