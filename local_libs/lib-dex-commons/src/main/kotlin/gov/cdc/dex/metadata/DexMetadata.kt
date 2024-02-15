package gov.cdc.dex.metadata

data class DexMetadata(
    val provenance: Provenance,
    var stage: StageMetadata
)
