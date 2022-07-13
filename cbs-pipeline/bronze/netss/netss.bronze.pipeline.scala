// Databricks notebook source
// MAGIC %run "../../common/config"

// COMMAND ----------

// MAGIC %run "../common/common.functions"

// COMMAND ----------

// MAGIC %run "./netss.bronze.functions"

// COMMAND ----------

import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.functions._

// COMMAND ----------

// Ingest Files
val fileIngestDf = spark.readStream.format("cloudFiles")
  .option("cloudFiles.format", "binaryFile")
  .load(Env.ingressNETSSPath)

val bronzeCommon = new BronzeCommon(spark)

bronzeCommon.createRawDFFromBinary(fileIngestDf).writeStream.format("delta")
  .option("checkpointLocation", Env.deltaNETSSBronzeRawCheckpoint)
  .option("path", Env.deltaNETSSBronzeRawPath)
  .trigger(Trigger.Once()) 
  .start()

// COMMAND ----------

val bronzeNETSS = new BronzeNETSS(spark)

// COMMAND ----------

// Debatch Files
val rawDf = spark.read
            .format("delta")
            .load(Env.deltaNETSSBronzeRawPath)

bronzeNETSS.debatch(rawDf).write.format("delta")
  .option("checkpointLocation", Env.deltaNETSSBronzeDebatchedCheckpoint)
  .option("path", Env.deltaNETSSBronzeDebatchedPath)
  .option("mergeSchema", "true")
  .mode("overwrite")
  .save
