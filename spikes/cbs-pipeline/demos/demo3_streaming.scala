// Databricks notebook source
val baseLoc =  "TODO"

val checkPointLoc =  "TODO"

val saveLoc =  "TODO"
val saveLoc2 =  "TODO"

// COMMAND ----------

import org.apache.spark.sql.functions._

// COMMAND ----------


val fileIngestDf = spark.read.format("text").load(baseLoc)

val fileRawDf = fileIngestDf.withColumn("decoded", decode($"value", "UTF-8"))                
                            .withColumn("__meta_ingestTimestamp", current_timestamp)
                            .withColumn("__meta_ingestUuid", expr("uuid()"))

println("text rows count: --> " + fileIngestDf.count)

//fileRawDf.show
fileRawDf.write.format("delta")
   .mode(SaveMode.Overwrite)
   .save(saveLoc)

// COMMAND ----------

import org.apache.spark.sql.streaming.Trigger
val getFileName = udf ( (path: String)=> path.split("/").last )

val fileIngestDf2 = spark.readStream.format("cloudFiles")
  .option("cloudFiles.format", "binaryFile")
  .load(baseLoc)

val fileRawDf2 = fileIngestDf2.withColumn("decoded", decode($"content", "UTF-8"))
                            .withColumn("__meta_fileName", getFileName($"path"))
                            .withColumn("__meta_ingestTimestamp", current_timestamp)
                            .withColumn("__meta_ingestUuid", expr("uuid()"))

fileRawDf2.writeStream.format("delta")
  .option("checkpointLocation", checkPointLoc)
 // .trigger(Trigger.Once())  // example streaming continuosly:  
.trigger(Trigger.ProcessingTime("30 seconds")) 

// COMMAND ----------


