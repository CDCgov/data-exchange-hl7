package test

import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.ProcessMetadata

data class MockMetadata(override val status: String?, val eventHubMD: EventHubMetadata): ProcessMetadata("MOCK", "0.0.1", status, eventHubMD) {
    var oneMoreProperty: String? = null
}