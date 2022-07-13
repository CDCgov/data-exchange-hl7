// Databricks notebook source
// MAGIC %run ../common/funcs

// COMMAND ----------

// MAGIC %run ../common/adfParams

// COMMAND ----------

import org.apache.spark.sql.DataFrame

class MmgLoader {
    import MmgLoader._

    def getAllDf(): DataFrame = {
      mmgAllDf
    } // .getDf

} // .MmgLoader

// DataBrick specific MMG Loader:
object MmgLoader {
  // TODO: make dbName dependency explicit
  
  val mmgTableRef = dbName + "." + mmgTableName //
  
  val mmgAllDf = spark.read.format("delta").table(mmgTableRef)
      .withColumn("element_name_common", Funcs.renameColHeader($"element_name") )
      .withColumn("block_name_common",  Funcs.renameColHeader($"block_name") )
      .drop("__mmg_meta_ingestTimestamp", "__mmg_meta_ingestUuid")
  
} // .MmgLoaderDbx

// COMMAND ----------


