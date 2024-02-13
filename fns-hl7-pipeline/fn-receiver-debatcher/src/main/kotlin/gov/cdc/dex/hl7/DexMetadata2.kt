package gov.cdc.dex.hl7

import gov.cdc.dex.metadata.ProcessMetadata
import gov.cdc.dex.metadata.Provenance

data class DexMetadata2(
       val provenance: Provenance,
      val processes: List<ProcessMetadata2>
    )
