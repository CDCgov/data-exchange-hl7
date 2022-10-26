package gov.cdc.dex.hl7.receiver

import gov.cdc.dex.metadata.ProcessMetadata

data class ReceiverProcessMetadata (override val status: String?): ProcessMetadata(RECEIVER_PROCESS, RECEIVER_VERSION , status) {
    companion object  {
        const val RECEIVER_PROCESS = "RECEIVER"
        const val RECEIVER_VERSION = "1.0.0"
    }
    var oneMoreProperty: String? = null
}