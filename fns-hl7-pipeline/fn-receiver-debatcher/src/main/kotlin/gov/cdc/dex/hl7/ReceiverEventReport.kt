package gov.cdc.dex.hl7
import com.google.gson.annotations.SerializedName

data class ReceiverEventReport(
    @SerializedName("file_name") val fileName : String,
    @SerializedName("file_uuid") val fileID: String,
    @SerializedName("message_batch") var messageBatch : String = "SINGLE",
    @SerializedName("number_of_messages") var totalMessageCount: Int = 0,
    @SerializedName("number_of_valid_messages") var validMessageCount: Int = 0,
    @SerializedName("number_of_messages_not_propagated") var errorMessageCount: Int = 0,
    @SerializedName("error_messages") val errorMessages: MutableList<ReceiverEventError> = mutableListOf()
)

data class ReceiverEventError (
    @SerializedName("message_index") val messageIndex: Int,
    @SerializedName("message_uuid") val messageUUID: String?,
    @SerializedName("error_message") val error: String?
        )