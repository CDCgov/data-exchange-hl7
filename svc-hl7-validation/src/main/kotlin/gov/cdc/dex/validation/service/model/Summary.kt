package gov.cdc.dex.validation.service.model
import com.google.gson.annotations.SerializedName

data class Summary(
    @SerializedName("valid_messages") val validMessages : Int,
    @SerializedName("invalid_messages") val invalidMessages : Int,
    @SerializedName("error_counts") val errors : ErrorCounts
    )

data class ErrorCounts (
    @SerializedName("total") val totalErrors: Int,
    @SerializedName("by_type") val errorsByType: Map<String, Int>,
    @SerializedName("by_category") val errorsByCategory: Map<String, Int>,
    @SerializedName("by_path") val errorsByPath : Map<String, Int>,
    @SerializedName("by_message") val errorsByMessage : Map<String, Int>
    )
