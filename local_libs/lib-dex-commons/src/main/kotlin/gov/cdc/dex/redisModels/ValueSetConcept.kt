package gov.cdc.dex.redisModels

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class ValueSetConcept(val id: String, val codeSystemOid: String, val valueSetVersionId: String,
    val conceptCode: String, val status: String, val statusDate: String, val cdcPreferredDesignation: String,
    val codeSystemConceptName: String)
