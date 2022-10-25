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

// MAGIC %run ../common/adfParams

// COMMAND ----------

// MAGIC %md # Loading Functions

// COMMAND ----------

// MAGIC %run ../mmg/fn_mmg

// COMMAND ----------

// MAGIC %md # Executing Pipeline

// COMMAND ----------

// DBTITLE 1,Load Data
val mmgDF = loadAndSaveMMG(mmgFolder, dbName)
