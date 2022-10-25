// Databricks notebook source
// MAGIC %md # Transformation Function

// COMMAND ----------

import open.HL7PET.tools.HL7StaticParser

val extractHL7FieldFirstValue = udf((message: String, field: String) => {
//   val parser = new HL7ParseUtils(message)
  val result = HL7StaticParser.getFirstValue(message, field)
  result map { _.trim() } 
})

val extractHL7Field = udf((message: String, field: String) => {
//   val parser = new HL7ParseUtils(message)
  val result = HL7StaticParser.getValue(message, field, false)
//   if (result.isPresent) {
  val first = result.getOrElse(null)
  if (first != null)
    first(0)
  else 
     null
})

// COMMAND ----------

// Functiion to read Transformation configuraiton file from /mnt/Config
val __readXformConfig = (filename: String) => {
 val xformConfigDF = spark.read
      .option("multiline","true")
      .json(filename)
  xformConfigDF
}


// COMMAND ----------

//Main Fucntion to Transform HL& into Flat schema:
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Column}

val transformData = (DF: DataFrame, configFilename: String, hl7Column: Column) => {
  val configDF = __readXformConfig(configFilename)
  val newDF = configDF.columns.foldLeft(DF){ (acc, c) => {
    val v = configDF.select(col(c)).first
    val path: String = v(0).asInstanceOf[String]
    if (path.contains("[*]"))
        acc.withColumn(c, extractHL7Field(hl7Column, lit(path)))
    else 
        acc.withColumn(c, extractHL7FieldFirstValue(hl7Column, lit(path)))
    
  }}
  newDF
}

// COMMAND ----------

// MAGIC %md ## END fn_transform
