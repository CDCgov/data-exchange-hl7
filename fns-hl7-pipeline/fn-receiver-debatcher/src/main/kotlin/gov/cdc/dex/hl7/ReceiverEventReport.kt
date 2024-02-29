package gov.cdc.dex.hl7
import com.google.gson.annotations.SerializedName
import gov.cdc.dex.metadata.RoutingMetadata

data class ReceiverEventReport(
    @SerializedName("schema_version") val schemaVersion: String = "0.0.1",
    @SerializedName("schema_name") val schemaName: String = "hl7_debatch_report",
    @SerializedName("routing_metadata") var routingData: RoutingMetadata? = null,
    @SerializedName("single_or_batch") var messageBatch : String = "SINGLE",
    @SerializedName("number_of_messages") var totalMessageCount: Int = 0,
    @SerializedName("number_of_messages_not_propagated") var notPropogatedCount: Int = 0,
    @SerializedName("error_messages") val errorMessages: MutableList<ReceiverEventError> = mutableListOf(),
    @SerializedName("stage") val metadata: MockedStage = MockedStage(),

)

data class ReceiverEventError (
    @SerializedName("message_index") val messageIndex: Int,
    @SerializedName("message_uuid") val messageUUID: String?,
    @SerializedName("error_message") val error: String?
        )

// "mocked" stage metadata simulates structure of StageMetadata with only the
// elements needed for the Processing Status API.
data class MockedStage (
    @SerializedName("stage_name") val stageName: String = "RECEIVER"
)