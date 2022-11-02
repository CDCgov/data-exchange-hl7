package test

import gov.cdc.dex.metadata.ProcessMetadata

data class MockMetadata(override val status: String?): ProcessMetadata("MOCK", "0.0.1", status) {
    var oneMoreProperty: String? = null
}