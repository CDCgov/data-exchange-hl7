// Databricks notebook source
// DBTITLE 1,UDFs 
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.functions._
import org.apache.spark.sql._

val getFileName = udf ( (path: String)=> path.split("/").last )


// COMMAND ----------

// MAGIC %md # Function loadHL7
// MAGIC 
// MAGIC Function to Read HL7 Data and save it at the bronze tables.

// COMMAND ----------

// DBTITLE 1,Transformation Function
def addMetadata(df: DataFrame): DataFrame = {
   val xformedDF = df.withColumn("decoded", decode($"content", "UTF-8"))
                            .withColumn("__meta_filename", getFileName($"path"))
                            .withColumn("__meta_ingest_timestamp", current_timestamp)
                            .withColumn("__meta_ingest_uuid", expr("uuid()"))
                            .withColumnRenamed("modificationTime" , "__meta_file_timestamp")
                            .withColumnRenamed("path", "__meta_file_path")
                            .withColumnRenamed("length", "__meta_file_size")
                           
  val finalDF = xformedDF.select(struct($"__meta_file_path", $"__meta_fileName", $"__meta_file_timestamp", $"__meta_ingest_timestamp", $"__meta_ingest_uuid", $"__meta_file_size").as("__metadata"), $"decoded")
  return finalDF
}

// COMMAND ----------

// DBTITLE 1,Pipeline Function
def loadAndSaveHL7(ingressFolder: String, tableRef: String, checkpointLocation: String): DataFrame = {
  
  val fileIngestDf = spark.readStream.format("cloudFiles")
      .option("cloudFiles.format", "binaryFile")
      .load(ingressFolder)
  
  val fileRawDF = addMetadata(fileIngestDf)
  
  Lake.writeRefDf(fileRawDF, tableRef, checkpointLocation, "", isPipelineStreaming)
  
  
//   spark.streams.awaitAnyTermination()
    
   return fileRawDF   
}
