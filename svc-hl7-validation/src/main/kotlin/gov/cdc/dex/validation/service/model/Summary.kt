package gov.cdc.dex.validation.service.model
import com.google.gson.annotations.SerializedName
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name="Summary", description = "A summary of all the validation reports generated by a batch submission.")
data class Summary(
    @field:Schema(description = "Total number of messages found in the batch submission")
    @SerializedName("total_messages")
    val totalMessages: Int,

    @field:Schema(description = "Number of messages that were found to be valid (no errors, but may have warnings)")
    @SerializedName("valid_messages")
    val validMessages : Int,

    @field:Schema(description = "Number of messages that had validation errors")
    @SerializedName("invalid_messages")
    val invalidMessages : Int,

    @field:Schema(name = "error_counts", implementation = ErrorCounts::class)
    @SerializedName("error_counts")
    val errors : ErrorCounts

    )

@Schema(name="error_counts", description = "Totals of error counts occurring in a batch submission, broken out by type, category, path, and message number.")
data class ErrorCounts (
    @field:Schema(name = "total", description = "Total of all errors in the batch")
    @SerializedName("total")
    val totalErrors: Int,

    @field:Schema(name = "by_type", description = "Errors in the batch by error type")
    @SerializedName("by_type")
    val errorsByType: Map<String, Int>,

    @field:Schema(name = "by_category", description = "Errors in the batch by error category")
    @SerializedName("by_category")
    val errorsByCategory: Map<String, Int>,

    @field:Schema(name = "by_path", description = "Errors in the batch by HL7 path")
    @SerializedName("by_path")
    val errorsByPath : Map<String, Int>,

    @field:Schema(name = "by_message", description = "Errors in the batch by message number", examples = ["message1:0", "message2:3"])
    @SerializedName("by_message")
    val errorsByMessage : Map<String, Int>
    )
