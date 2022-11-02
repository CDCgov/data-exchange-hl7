// Databricks notebook source
// MAGIC %run ./fn_lake

// COMMAND ----------


// COMMAND ----------

// TODO: populate defaults!

dbutils.widgets.text("tablePrefix", "TODO") // TABLES PREFIX
dbutils.widgets.text("dbName", "TODO")
dbutils.widgets.text("deltaFolder", "TODO")
dbutils.widgets.text("ingressFolder", "TODO" )
dbutils.widgets.text("mmgFolder", "TODO" )  
//vocabulary
dbutils.widgets.text("vocabListFile", "TODO")
dbutils.widgets.text("vocabIngressFolder", "TODO")


val tablePrefix = dbutils.widgets.get("tablePrefix")
val dbName = dbutils.widgets.get("dbName")
val mntDelta = dbutils.widgets.get("deltaFolder")
val ingressFolder = dbutils.widgets.get("ingressFolder")
val mmgFolder = dbutils.widgets.get("mmgFolder")

val vocabListFile = dbutils.widgets.get("vocabListFile")
val vocabIngressFolder = dbutils.widgets.get("vocabIngressFolder")




// COMMAND ----------

// DBTITLE 1,Table names for our CBSP lake
val cbsTable  = "cbs_bronze_hl7_"
val rawTable       = cbsTable  + tablePrefix + "_raw"
val debatchedTable = cbsTable  + tablePrefix + "_debatched"
val validatedTable = cbsTable  + tablePrefix + "_validated"

val silverOBXLakeAndSegments = "cbs_silver_hl7_" + tablePrefix + "_obxlakeandsegments"
val goldTable = "cbs_gold_hl7_" + tablePrefix
val goldWithContValRep = "cbs_gold_hl7_cvr_" + tablePrefix
val goldWithFlag = "cbs_gold_hl7_flag_" + tablePrefix

val mmgTableName = "cbs_message_mapping_guides"
val vocabTableName = "cbs_content_vocabulary"


// COMMAND ----------

val isPipelineStreaming = false // dbutils.widgets.text("tablePrefix", "dev1")

val ingressTableName = "TODO"


