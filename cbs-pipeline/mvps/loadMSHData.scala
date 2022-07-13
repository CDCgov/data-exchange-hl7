// Databricks notebook source
// MAGIC %run ./fn_transform

// COMMAND ----------

// MAGIC %run ../common/fn_lake

// COMMAND ----------

val validatedTable = "cbs_bronze_hl7_dev1_validated"

val messagesDf = spark.read
    .format("delta")
    .table(Lake.getTableRef( "davt_xlr_cbsp_dev",  "cbs_bronze_hl7_dev1_validated"))

// COMMAND ----------

val mshDF =  transformData(messagesDf,  "TODO/MSH-transform.json", messagesDf.col("message_text"))
display(mshDF)

// COMMAND ----------

val msh21DF =  transformData(messagesDf,  "TODO/MSH21-Transform.json", messagesDf.col("message_text"))
display(msh21DF.select($"__metadata", explode($"profile")))

// COMMAND ----------

// mshDF.withColumn("msh_7", to_date($"msh_7_str"))

// COMMAND ----------

//SAVE data
// spark.write.table(...)

// COMMAND ----------

val pidDF =  transformData(messagesDf,  "TODO/PID-Transform.json", messagesDf.col("message_text"))
display(pidDF)

// COMMAND ----------


