// Databricks notebook source
// MAGIC %md # Load Functions

// COMMAND ----------

// MAGIC %run ../common/adfParams

// COMMAND ----------

// MAGIC %run ../silver/hl7/fn_hl7_to_obxs_and_segm

// COMMAND ----------

val messagesDf = Lake.readDf(dbName, validatedTable, isPipelineStreaming)


// COMMAND ----------

val toObxsAndSegments = udf ( (hl7: String) => { 
  HL7ToObxsAndSegments(hl7)
}) // .toLakesAndSegments


// COMMAND ----------

val df2 = messagesDf.withColumn("lakes", toObxsAndSegments($"message_text")).select("__metadata", "__message_info", "lakes.*")
              .withColumnRenamed("_1", "obx_epi")
              .withColumnRenamed("_2", "obx_nonepi")
              .withColumnRenamed("_3", "other_segments")

//display( df2 )


// COMMAND ----------

Lake.writeDf(df2, dbName, silverOBXLakeAndSegments, mntDelta, "", isPipelineStreaming)


