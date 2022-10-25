// Databricks notebook source
val baseLoc =  "TODO"

val fileLoc = baseLoc + "Genv2_2-0-1_TC01.txt"
val mmgLoc = baseLoc + "Generic_MMG_V2.0.json"

println("file location: --> " + fileLoc)
println("mmg location: --> " + mmgLoc)


// COMMAND ----------

val df1 = spark.read.format("text").load(fileLoc)

display( df1 )

// COMMAND ----------

df1.columns.size

df1.schema

// COMMAND ----------

val df2 = df1.filter( $"value".startsWith("OBX"))

display( df2 )

// COMMAND ----------

import org.apache.spark.sql.functions._

val df3 = df2.withColumn( "list", split($"value", "\\|") )
          .withColumn("Obx41", element_at($"list", 4))
          .withColumn("Data", element_at($"list", 6))
          .drop("value", "list")
          .withColumn("Obx41Segments", split($"Obx41", "\\^"))
          .withColumn("Id", element_at($"Obx41Segments", 1))
          .drop("Obx41", "Obx41Segments")
          .select("Id", "Data")

display( df3 )

// COMMAND ----------

val mmg1 = spark.read.format("json").option("multiline", "true").load(mmgLoc).select( "result" )

  .select( explode($"result.blocks").as("block") ).withColumn("elements", $"block.elements")
  .select(explode($"elements").as("element"))
      .withColumn("element_name", $"element.name")
      .withColumn("element_identifier", $"element.mappings.hl7v251.identifier")
      .withColumn("element_type", $"element.mappings.hl7v251.segmentType")
  .select("element_name", "element_identifier", "element_type")
  .filter($"element_type" === "OBX")

display( mmg1 )

// COMMAND ----------

val df4 = df3.join(mmg1, df3("Id") === mmg1("element_identifier"), "inner").select("element_name", "Data")
display( df4 )

// COMMAND ----------

val df5 = df4.groupBy().pivot("element_name").agg(collect_set("Data"))

display( df5 )

// COMMAND ----------


