// Databricks notebook source
// DBTITLE 1,UDFs
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql._


// val extractFhsFts = udf ( (fileText: String) => {
//   fileText.split("\\\r\\\n|\\\n\\\r|\\\r|\\\n").filter(x => {
//      x.startsWith("FHS") || x.startsWith("FTS")
//   }) 
// }) // .extractFhsFts

val NEW_LINE = "\\\r\\\n|\\\n\\\r|\\\r|\\\n"

val extractHeader = (fileText: String, seg: String) => {
  fileText.split(NEW_LINE).find(x => {
    x.startsWith(seg)
  })
}

val extractFhs = udf ( (fileText: String) => {
  extractHeader(fileText, "FHS")
}) // .extractFhsFts

val extractFts = udf ( (fileText: String) => {
  extractHeader(fileText, "FTS")
}) // .extractFhsFts

val extractBhs = udf ( (fileText: String) => {
  extractHeader(fileText, "BHS") 
}) // .extractBhsBts

val extractBts = udf ( (fileText: String) => {
  extractHeader(fileText, "BTS")
}) // .extractBhsBts

val debatchMshWithIndex = udf ( (fileText: String) => fileText.split("((?=MSH))").zipWithIndex.map{ case (mshText, mshCount) => (mshCount, mshText) } )

val getMshIndex = udf ( (s: Tuple2[Int, String]) => s._1 )
val getMshText = udf ( (s: Tuple2[Int, String]) => s._2 )

// COMMAND ----------

// MAGIC %md # Function debatchHL7
// MAGIC 
// MAGIC Function to debatch messages:

// COMMAND ----------

// DBTITLE 1,Transformation Function
def debatchHL7(df: DataFrame): DataFrame = {
  val debatched = df.withColumn("MessageArrTup", debatchMshWithIndex($"decoded"))
                      
                      .withColumn("MessageTup", explode($"MessageArrTup"))
                      .withColumn("__meta_message_index", getMshIndex($"MessageTup"))
                      .withColumn("message_text", getMshText($"MessageTup"))
                      .drop("MessageArrTup", "MessageTup")
                      
                      .filter($"message_text".startsWith("MSH"))
                      .withColumn("FHS", extractFhs($"decoded"))
                      .withColumn("FTS", extractFts($"decoded"))
                      .withColumn("BHS", extractBhs($"decoded"))
                      .withColumn("BTS", extractBts($"decoded"))
//                       .filter( size($"FHS") === 0 ||  size($"FHS") === 2 ) // valid
//                       .filter( size($"BHS") === 0 ||  size($"BHS") === 2 ) // valid
//                       .withColumn("__meta_record_uuid", expr("uuid()"))
                      .withColumn("__metadata", struct($"__metadata.*", expr("uuid()").alias("__meta_record_uuid"), $"__meta_message_index"))
                      .withColumn("file_header", struct("FHS", "FTS"))
                      .withColumn("batch_header", struct("BHS", "BTS"))
                      .drop("decoded", "FHS", "FTS", "BHS", "BTS", "__meta_message_index")
  return debatched 
}

// COMMAND ----------

// DBTITLE 1,Pipeline Function
def debatchAndSaveHL7(inputDF: DataFrame, tableRef: String, checkpointLocation: String ): DataFrame = {
  
  val debatched = debatchHL7(inputDF)
  
  Lake.writeRefDf(debatched, tableRef, checkpointLocation, "", isPipelineStreaming)

  
   return debatched
}
