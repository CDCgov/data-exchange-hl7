// Databricks notebook source
// Loading Vocabulary Mapping CSV file into Frame

var container = "TODO"
var storageAcct = "TODO"
var rd_format = "csv"

var mnt_path = "abfss://" + container + "@" + storageAcct + "TODO"  
var temp_tbl_name = "vocab_mapping"
var df_vmap = spark.read.format(rd_format).option("header","true").option("inferSchema", "false").load(mnt_path)
    .toDF("value_set_oid","value_set_code","code_system_oid")
  

// COMMAND ----------

// Loading Content Vocabulary CSV files into data Frame
var mnt_path = "abfss://" + container + "@" + storageAcct + "TODO"  

//rd_format =  "com.crealytics.spark.excel"
//var df = spark.read.format(rd_format).option("header",true).option("inferSchema", "true").load(mnt_path)
var df = spark.read.format(rd_format).option("header","true").option("inferSchema", "false").load(mnt_path)
      .toDF("concept_code","concept_name","pref_concept_name","pref_alternate_code","code_system_oid","code_system_name","code_system_code","code_system_version","hl7_table_0396_code")

// COMMAND ----------

// Joining above Data frames and create a Delta Table

var delta_tbl_name = "davt_xlr_cbsp_dev.content_vocabulary"

df.join(df_vmap, df("code_system_oid") === df_vmap("code_system_oid"), "inner" ).drop(df_vmap("code_system_oid"))
  .write.mode("Overwrite").saveAsTable(delta_tbl_name)

//df.write.mode("Ignore").saveAsTable("davt_xlr_cbsp_dev.content_vocabulary")

//df_vmap.createOrReplaceTempView(temp_tbl_name)
//spark.sql("SELECT cvm.value_set_oid,  cvm.value_set_code, cv.* FROM " +
//            " davt_xlr_cbsp_dev.content_vocabulary_mapping cvm " +
//            " INNER JOIN  davt_xlr_cbsp_dev.content_vocabulary cv ON  cvm.code_system_oid = cv.code_system_oid").show(false)


//df.write.format(wrt_format).saveAsTable(delta_tbl_name)
//SELECT * FROM davt_xlr_cbsp_dev.content_vocabulary_delta;
//display(df)

// COMMAND ----------
