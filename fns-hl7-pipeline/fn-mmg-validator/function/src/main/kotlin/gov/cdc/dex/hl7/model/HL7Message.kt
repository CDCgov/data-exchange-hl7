package gov.cdc.dex.hl7.model

import com.google.gson.annotations.SerializedName

import gov.nist.validation.report.Entry

data class HL7Message (
  @SerializedName("content"  ) var content  : String,
  @SerializedName("metadata" ) var metadata : HL7MessageMetadata,
  @SerializedName("structure_validation_report" ) var structuralValidationReport : Map<String,List<Entry>>,
  // added in this fn
  @SerializedName("content_validation_report" ) var contentValidationReport : Map<String,String>? = null,

  ) // .HL7Message