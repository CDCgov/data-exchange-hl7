// Databricks notebook source
// MAGIC %md # Load Vocabulary
// MAGIC 
// MAGIC This pipelines loads all available vocabularies needed to perform content validation.
// MAGIC 
// MAGIC ## Requirements:
// MAGIC In order for this pipeline to work you need the following:
// MAGIC 
// MAGIC 1. create a csv file that lists all vocabularies to be loaded. This CSV must have the following columns: 
// MAGIC * value_set_oid	
// MAGIC * value_set_code	
// MAGIC * code_system_oid
// MAGIC 
// MAGIC     Note: The value in value_set_code must match the name of the value_set in the MMG definition in order for the pipeline to be able to match the correct value set to be validated.
// MAGIC 
// MAGIC 1. Download the value sets as csv from PhinVads and save them all under the same folder. ex.: /ingress/vocab
// MAGIC 
// MAGIC 1. provide the database where you would like to save the table with all valuesets.
// MAGIC 
// MAGIC The result of this pipeline is a table with name "content_validation" on the provided database.

// COMMAND ----------

// MAGIC %run ../common/adfParams

// COMMAND ----------

// MAGIC %run ../vocab/fn_vocabulary

// COMMAND ----------

val vocabDF = loadAndSaveVocabulary(vocabListFile, vocabIngressFolder, dbName, vocabTableName)
