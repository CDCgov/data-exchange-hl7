package com.example

import com.google.gson.annotations.SerializedName

import gov.nist.validation.report.Entry

data class HL7Message (

  @SerializedName("content"  ) var content  : String,
  @SerializedName("metadata" ) var metadata : HL7MessageMetadata,
  @SerializedName("structuralValidationReport" ) var structuralValidationReport : Map<String,List<Entry>>,
  // added in this fn
  @SerializedName("contentValidationReport" ) var contentValidationReport : Map<String,String>? = null,  
  
) // .HL7Message