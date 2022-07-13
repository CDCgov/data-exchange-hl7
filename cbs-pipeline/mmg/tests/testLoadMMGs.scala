// Databricks notebook source
// MAGIC %run ../fn_mmg

// COMMAND ----------

val oneMMG = new MMGLoader("davt_xlr_cbsp_dev").loadMMG("Malaria_MMG_V1.0")
display(oneMMG)

// COMMAND ----------

val allMMGs = new MMGLoader("davt_xlr_cbsp_dev").getAllMMGsWithMap()
display(allMMGs)

// COMMAND ----------

display(allMMGs.select("profileIdentifier", "mmg_seq.element_name"))

// COMMAND ----------


