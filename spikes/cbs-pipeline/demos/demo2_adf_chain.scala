// Databricks notebook source
val first = System.currentTimeMillis

// COMMAND ----------

// MAGIC %run ./demo2_adf_notebook_1

// COMMAND ----------

// MAGIC %run ./demo2_adf_notebook_2

// COMMAND ----------

// MAGIC %run ./demo2_adf_notebook_1

// COMMAND ----------

// MAGIC %run ./demo2_adf_notebook_2

// COMMAND ----------

// MAGIC %run ./demo2_adf_notebook_1

// COMMAND ----------

// MAGIC %run ./demo2_adf_notebook_2

// COMMAND ----------

// MAGIC %run ./demo2_adf_notebook_1

// COMMAND ----------

// MAGIC %run ./demo2_adf_notebook_2

// COMMAND ----------

// MAGIC %run ./demo2_adf_notebook_1

// COMMAND ----------

// MAGIC %run ./demo2_adf_notebook_2

// COMMAND ----------

val last = System.currentTimeMillis

val totalDuration = last - first

println( s"total duration $totalDuration ms")

// COMMAND ----------


