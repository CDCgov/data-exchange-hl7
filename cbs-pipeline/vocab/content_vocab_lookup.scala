// Databricks notebook source
object ContVocDev {
  
  val tbl_name = "content_vocabulary"
  val YNU_id = "2.16.840.1.113883.12.136"
  val YNU = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid = ('" + YNU_id +"')").collect().toList
//  val YNU = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev.bronze_content_vocab WHERE code_system_oid='2.16.840.1.113883.12.136'").collect().toList
  val MFU = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid= '2.16.840.1.113883.12.1'").collect().toList
  val Race = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +"  WHERE code_system_oid='2.16.840.1.113883.6.238'").collect().toList
  val Ethnicity = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +"  WHERE code_system_oid='2.16.840.1.113883.6.238'").collect().toList
  val DurationUnit = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.113883.6.8'").collect().toList
  val AgeUnit = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.113883.6.8'").collect().toList
  val DiseaseAcquiredJurisdiction = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.114222.4.5.274'").collect().toList
  val CaseClassStatus = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.113883.6.96'").collect().toList
  val CaseTransmissionMode = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.113883.6.96'").collect().toList
  val ResultStatus = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.113883.12.85'").collect().toList
  val BinationalReportingCriteria = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.114222.4.5.274'").collect().toList
  val NationalReportingJurisdiction = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.113883.6.92'").collect().toList
  val YNRD = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid=''").collect().toList
  val TemperatureUnit = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.113883.6.8'").collect().toList
  val BloodProduct = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.113883.6.96'").collect().toList
  val OutdoorActivities = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.113883.6.96'").collect().toList
  val LabTestInterpretation = spark.sql("SELECT concept_code, concept_name, hl7_table_0396_code FROM davt_xlr_cbsp_dev." +tbl_name +" WHERE code_system_oid='2.16.840.1.113883.6.96'").collect().toList
  
 
}

// COMMAND ----------

println(ContVocDev.YNU)
println(ContVocDev.MFU)
println (ContVocDev.Race)
println (ContVocDev.Ethnicity)
println (ContVocDev.DurationUnit)
println (ContVocDev.AgeUnit)
println (ContVocDev.DiseaseAcquiredJurisdiction)
println (ContVocDev.CaseClassStatus)
println (ContVocDev.CaseTransmissionMode)
println (ContVocDev.ResultStatus)
println (ContVocDev.BinationalReportingCriteria)
println (ContVocDev.NationalReportingJurisdiction)
println (ContVocDev.YNRD)
println (ContVocDev.TemperatureUnit)
println (ContVocDev.BloodProduct)
println (ContVocDev.OutdoorActivities)
println (ContVocDev.LabTestInterpretation)

// COMMAND ----------



// COMMAND ----------


