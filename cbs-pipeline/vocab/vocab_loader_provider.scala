// Databricks notebook source
// MAGIC %run ../common/adfParams

// COMMAND ----------

import org.apache.spark.sql.DataFrame 

class VocabLoader {
    import VocabLoader._

    def getAllDf(): DataFrame = {
      vocabAllDf
    } // .getAllDf

} // .VocabLoader

// DataBrick specific MMG Loader:
object VocabLoader {
  
  val vocabDbTable = Lake.getTableRef(dbName, vocabTableName)
  
  val vocabAllDf = spark.sql("SELECT * FROM " + vocabDbTable)
   // .select("code_system_code", "hl7_table_0396_code", "code_system_oid", "concept_code")
  
} // .VocabLoader

// COMMAND ----------

class VocabProvider(vocabLdr: VocabLoader) {
    
  def getAllMap(): Map[String, Seq[(String, String, String)] ] = {
    
    vocabLdr.getAllDf.select("value_set_code", "hl7_table_0396_code", "code_system_oid", "concept_code")
                     .filter($"value_set_code".isNotNull)
                     .collect().map( row => (row(0).toString, (row(1).toString, row(2).toString, row(3).toString ) )).groupBy(_._1).map{ case(k,v) => k -> v.map(_._2).toSeq } 
    
  } // .getAllMap

} // .VocabProvider

// COMMAND ----------

// display vocab df
val vocabDf = (new VocabLoader).getAllDf
// display( vocabDf )

// COMMAND ----------

// try vocabs available
val vocabMap = new VocabProvider(new VocabLoader).getAllMap

val vocabsAvailable = vocabMap.keySet
// println($"vocabsAvailable: -> $vocabsAvailable")
// println("-------------------------------------")

// COMMAND ----------


