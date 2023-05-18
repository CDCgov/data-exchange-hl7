package gov.cdc.dex.metadata

import com.google.gson.annotations.SerializedName

data class DexMessageInfo (
    @SerializedName("event_code") var eventCode: String?,
    @SerializedName("route") var route: String?,
    @SerializedName("mmgs") var mmgKeyList: List<String>?,
    @SerializedName("reporting_jurisdiction") var jurisdictionCode: String?,
    @SerializedName("type") var type: HL7MessageType
)

enum class HL7MessageType {
    CASE, ELR, UNKNOWN
}
