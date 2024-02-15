package gov.cdc.dex.hl7

import com.google.gson.annotations.SerializedName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.ProcessMetadata
import java.text.SimpleDateFormat
import java.util.*
import gov.cdc.dex.metadata.StageMetadata

data class ReceiverProcessMetadata (override val status: String?,
                                    @Transient val eventHubMD: EventHubMetadata): StageMetadata(
    RECEIVER_PROCESS, RECEIVER_VERSION , status, eventHubMD, listOf()) {
    companion object  {
        const val RECEIVER_PROCESS = "RECEIVER"
        const val RECEIVER_VERSION = "1.0.0"
    }
}


data class ReceiverProcessMetadata2 (override val status: String?,   @Transient val eventGridMD: String): ProcessMetadata2(
    RECEIVER_PROCESS, RECEIVER_VERSION , status, eventGridMD, listOf()) {
    companion object  {
        const val RECEIVER_PROCESS = "RECEIVER"
        const val RECEIVER_VERSION = "1.0.0"
    }
}