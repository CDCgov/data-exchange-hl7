package gov.cdc.dex.metadata

data class DexMetadata(
    val provenance: Provenance,
    val processes: List<ProcessMetadata>
)
