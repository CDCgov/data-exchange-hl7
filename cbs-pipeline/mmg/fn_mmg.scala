// Databricks notebook source
// MAGIC %run ../common/fn_lake

// COMMAND ----------

// MAGIC %run ../common/funcs

// COMMAND ----------

val mmgTableName = "cbs_message_mapping_guides"

val MMG_ELEM_MISSING_LONG = -1

// COMMAND ----------

// DBTITLE 1,UDFs

import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.functions._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Column

def subfield(col: Column, subfield: Int): Column = {
  // utility function for selecting subfields of hl7 message
  split(col, "\\^").getItem(subfield)
} // .subfield

// COMMAND ----------

// DBTITLE 1,Pipeline functions

import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.functions._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Column

//Method to load All MMGs from a given folder, overwriting all data 
def loadAndSaveMMG(mmgFolder: String, dbName: String): DataFrame = {
  val fileIngestDf = spark.read.option("multiline", "true").json(mmgFolder)
                    .selectExpr( "result")
  
//   flattens the mmg Json structure to a Dataframe which can be later joined to OBX
  val transformedMMG = fileIngestDf.select(explode($"result.blocks"), subfield($"result.profileIdentifier", 0).as("profileIdentifier")).filter(size($"col.elements") =!= 0)
    .withColumn("elements", $"col.elements")
    .withColumn("block_ordinal", $"col.ordinal")
    .withColumn("block_name", $"col.name")
    .withColumn("block_type", $"col.type")
    .withColumn("block_id", $"col.id")
    .select($"profileIdentifier", $"block_type", $"block_name",$"block_ordinal", $"block_id", explode($"elements"))
        .withColumn("element_name", $"col.name")
        .withColumn("element_ordinal", $"col.ordinal")
        .withColumn("element_dataType", $"col.dataType")
        .withColumn("element_codeSystem", $"col.codeSystem")
        .withColumn("element_isRepeat", $"col.isRepeat")
        .withColumn("element_valueSetCode", $"col.valueSetCode")
        .withColumn("element_valueSetVersionNumber", $"col.valueSetVersionNumber")
        .withColumn("element_identifier", $"col.mappings.hl7v251.identifier")
        .withColumn("element_segmentType", $"col.mappings.hl7v251.segmentType")
        .withColumn("element_fieldPosition", $"col.mappings.hl7v251.fieldPosition")
        .withColumn("element_componentPosition", $"col.mappings.hl7v251.componentPosition")
        .withColumn("element_cardinality", $"col.mappings.hl7v251.cardinality")
        .withColumn("element_hl7v2DataType", $"col.mappings.hl7v251.dataType")
  .drop("col")


  val MMGtoWrite = transformedMMG.withColumn("__meta_ingest_timestamp", current_timestamp)
                        .withColumn("__meta_ingest_uuid", expr("uuid()"))

  
  MMGtoWrite.write.format("delta")
    .mode(SaveMode.Overwrite)
    .option("mergeSchema", "true")
    .saveAsTable(Lake.getTableRef(dbName, mmgTableName))

  return MMGtoWrite
} // .loadMMG

// COMMAND ----------

// DBTITLE 1,Other Functions

import org.apache.spark.sql.DataFrame 
import org.apache.spark.sql.functions._

class MMGLoader(dbName: String) {
  def loadMMGs(): DataFrame = {
    val mmgAllDf = spark.read.format("delta").table(Lake.getTableRef( dbName, mmgTableName))
    .select($"profileIdentifier", 
                       $"block_type", 
                       $"block_name", 
                       $"block_ordinal", 
                       $"block_id", 
                       $"element_name", 
                       $"element_ordinal", 
                       $"element_dataType", 
                       $"element_isRepeat", 
                       $"element_valueSetCode", 
                       $"element_valueSetVersionNumber",
                       $"element_identifier", 
                       $"element_segmentType",
                       $"element_fieldPosition", 
                       $"element_componentPosition", 
                       $"element_cardinality",
                       $"element_hl7v2DataType",
                       $"element_codeSystem")
       .withColumn("element_name_common", Funcs.renameColHeader($"element_name") )
      .withColumn("block_name_common",  Funcs.renameColHeader($"block_name") )
    return mmgAllDf.na.fill("").na.fill(MMG_ELEM_MISSING_LONG)
  }
  
  def loadMMG( profile: String): DataFrame = {
     loadMMGs()
      .filter(s"profileIdentifier == '$profile'")
  }
   def getAllMMGsWithMap(): DataFrame = {
     loadMMGs()
        .groupBy("profileIdentifier").agg(collect_list(struct($"*")).alias("mmg_seq"))
  }
}
