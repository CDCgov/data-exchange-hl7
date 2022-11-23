package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName

data class ValueSetConcept(

    @SerializedName("id") val id: String, 
    @SerializedName("codeSystemOid") val codeSystemOid: String, 
    @SerializedName("valueSetVersionId") val valueSetVersionId: String,
    @SerializedName("conceptCode") val conceptCode: String,
    @SerializedName("status") val status: String,
    @SerializedName("statusDate") val statusDate: String,
    @SerializedName("cdcPreferredDesignation") val cdcPreferredDesignation: String,
    @SerializedName("codeSystemConceptName") val codeSystemConceptName: String,
    @SerializedName("preferredConceptName") val preferredConceptName: String,
    @SerializedName("conceptName") val conceptName: String,

) // .ValueSetConcept
