// Databricks notebook source
// MAGIC %md # Load Vocabulary into table.

// COMMAND ----------

// MAGIC %run ../common/fn_lake

// COMMAND ----------

import org.apache.spark.sql.DataFrame


def loadAndSaveVocabulary(vocabListFile: String, vocabIngressFolder: String, dbName: String, tableName: String): DataFrame = {
  //Load List of Vocabularies
  val df_vmap = spark.read.format("csv")
      .option("header","true")
      .option("inferSchema", "false")
      .load(vocabListFile)
    .toDF("value_set_oid","value_set_code","code_system_oid")
  
  //Load Value Sets:
  val valueSetsDF = spark.read.format("csv")
      .option("header","true")
      .option("inferSchema", "false")
      .load(vocabIngressFolder)
     .toDF("concept_code","concept_name","pref_concept_name","pref_alternate_code","code_system_oid","code_system_name","code_system_code","code_system_version","hl7_table_0396_code")
  
  //Join Value Sets with ValueSetCode
  val finalDF = valueSetsDF.join(df_vmap, valueSetsDF("code_system_oid") === df_vmap("code_system_oid"), "inner" ).drop(df_vmap("code_system_oid"))
    
  
  finalDF.write.mode("Overwrite").saveAsTable(Lake.getTableRef(dbName, tableName))
  
  finalDF
}

// COMMAND ----------


