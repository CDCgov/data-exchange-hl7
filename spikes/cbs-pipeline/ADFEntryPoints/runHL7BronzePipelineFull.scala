// Databricks notebook source
// MAGIC %md # Intro
// MAGIC 
// MAGIC This is a very opinionated notebook that expects the folder structure to follow a specific format and generates tables based on a given naming schema.
// MAGIC 
// MAGIC Database must be created prior to running this notebook and it should be created with LOCATION specified.
// MAGIC 
// MAGIC The folder structure is as follows:
// MAGIC ```
// MAGIC /delta
// MAGIC   /databases
// MAGIC     {{dbName}}
// MAGIC         {{tables}} //tables will be created here.
// MAGIC   /checkpoints
// MAGIC     {{dbName}}
// MAGIC         {{tables}}  //checkpoint info will be saved here.
// MAGIC   
// MAGIC ```
// MAGIC 
// MAGIC The tables created will be as follow:
// MAGIC * cbs_bronze_hl7_{{tablePrefix}}_raw -> Holds the initial ingestion data with added metadata
// MAGIC * cbs_bronze_hl7_{{tablePrefix}}_debatched -> Debaches the messages and saves each message as its own record
// MAGIC * cbs_bronze_hl7_{{tablePrefix}}_validated -> Validates and stores the reports along with the message

// COMMAND ----------

// MAGIC %md # Loading Functions

// COMMAND ----------

// MAGIC %run ../common/adfParams

// COMMAND ----------

// MAGIC %run ../bronze/hl7/fn_load_hl7

// COMMAND ----------

// MAGIC %run ../bronze/hl7/fn_debatch_hl7

// COMMAND ----------

// MAGIC %run ../bronze/hl7/fn_validate_hl7

// COMMAND ----------

// MAGIC %md # Executing Pipeline

// COMMAND ----------

// DBTITLE 1,Load Data
val hl7DataDF = isPipelineStreaming match {
  
  case true => loadAndSaveHL7(ingressFolder, Lake.getTableRef(dbName, rawTable), Lake.getCheckpointLocation(mntDelta, dbName, rawTable))
  
  case _ => Lake.readDf(dbName, ingressTableName, isPipelineStreaming)
  
} // .match

// COMMAND ----------

// DBTITLE 1,Debatch Data
val debatchedDF = debatchAndSaveHL7(hl7DataDF,  Lake.getTableRef(dbName, debatchedTable), Lake.getCheckpointLocation(mntDelta, dbName, debatchedTable))

// COMMAND ----------

// DBTITLE 1,Validate Data
val validatedDF = validateAndSaveHL7(debatchedDF, Lake.getTableRef(dbName, validatedTable), Lake.getCheckpointLocation(mntDelta, dbName,validatedTable))

// COMMAND ----------


