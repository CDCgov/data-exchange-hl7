package test

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.ProcessMetadata

data class MockMetadata(override val status: String?, val eventHubMD: EventHubMetadata): ProcessMetadata("MOCK", "0.0.1", status, eventHubMD, listOf("NOTF_ORU_V3.2")) {
    var oneMoreProperty: String? = null
}