// Databricks notebook source
// https://github.com/mscaldas2012/HL7-PET
import open.HL7PET.tools.HL7StaticParser

val extractHL7FieldFirstValue = udf((message: String, field: String) => {
    // val parser = new HL7ParseUtils(message)
    val result = HL7StaticParser.getFirstValue(message, field)
    result
})


val extractHL7Field = udf((message: String, field: String) => {
    // val parser = new HL7ParseUtils(message)
    val result = HL7StaticParser.getValue(message, field, false).get
    result
})

// COMMAND ----------

import cdc.xlr.structurevalidator._
import scala.util.{Try, Failure, Success}
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit


val validateMessage = udf ( (message: String) => {
 val ftr = StructureValidator().validate(message) 
  
  Try( Await.result(ftr, Duration(2, TimeUnit.SECONDS)) ) match {
    case Success( report ) => report
    case Failure( err ) => "ERROR: " + err.getMessage
  } // .Try 
}) // .validateMessage


 

// COMMAND ----------

// DBTITLE 1,Transformation Function
import org.apache.spark.sql._

def validateHL7(df: DataFrame): DataFrame = {
   val newDF = df.withColumn("validation_results", validateMessage($"message_text"))
  return newDF
}

// COMMAND ----------


import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger

val CASEID_SEPARATOR = "~~"
val EPI_OBR = "68991-9"
val REPORTING_JUR = "77968-6"

def validateAndSaveHL7(inputDF: DataFrame, tableRef: String, checkpointLocation: String): DataFrame = {
    val debatchedWithValidationResult = validateHL7(inputDF)
                                            .withColumn("sending_facility",  extractHL7FieldFirstValue($"message_text", lit("MSH-4.1")) )
                                            .withColumn("msh",  extractHL7FieldFirstValue($"message_text", lit("MSH")) )
                                            .withColumn("pid",  extractHL7FieldFirstValue($"message_text", lit("PID")) )
                                            .withColumn("obr",  extractHL7FieldFirstValue($"message_text", lit("OBR")) )
                                            .withColumn("msh7",  extractHL7FieldFirstValue($"message_text", lit("MSH-7")) )
                                            .withColumn("__anly_case_id", concat( 
                                                extractHL7FieldFirstValue($"message_text", lit("OBR[@4.1='"+ EPI_OBR + "']-3.1")), 
                                                lit(CASEID_SEPARATOR), 
                                                extractHL7FieldFirstValue($"message_text", lit("OBX[@3.1='" + REPORTING_JUR + "']-5.1")) 
                                             ))
                                            .withColumn("profile_identifier", extractHL7FieldFirstValue($"message_text", lit("MSH-21[$LAST].1")))
                                             .withColumn("profile_identifier_all", extractHL7Field($"message_text", lit("MSH-21.1"))(0) )
                                             .select( $"__metadata", struct( $"sending_facility", $"profile_identifier", $"profile_identifier_all",$"__anly_case_id", $"msh7", $"msh", $"pid", $"obr").as("__message_info"),
                                                              $"message_text", $"file_header", $"batch_header", $"validation_results")
  
  //  def writeRefDf(df: DataFrame, tableRef: String, checkpointLocation: String, partitionBy: String, isStreaming: Boolean)
  Lake.writeRefDf(debatchedWithValidationResult, tableRef, checkpointLocation, "", isPipelineStreaming)
  
  
  debatchedWithValidationResult
  
}

// COMMAND ----------


