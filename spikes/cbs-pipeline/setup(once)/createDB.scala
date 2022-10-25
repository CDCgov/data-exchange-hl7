// Databricks notebook source
dbutils.widgets.dropdown("environment", "Dev", Seq("Dev", "Qa", "Prod"))

// COMMAND ----------

val currentEnv = "Qa"

// COMMAND ----------

// MAGIC %run ../common/config

// COMMAND ----------

val dbName = "example"
val mntDelta =  "TODO"  
// println(XLRLake.dbName)

// COMMAND ----------

spark.sql("CREATE DATABASE IF NOT EXISTS " + dbName + " LOCATION '" + mntDelta + "'")

// COMMAND ----------

val result = spark.sql("Describe database " + XLRLake.dbName)
display(result)
