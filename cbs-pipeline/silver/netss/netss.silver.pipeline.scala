// Databricks notebook source
// MAGIC %run "../../common/config"

// COMMAND ----------

// MAGIC %run "./netss.silver.functions"

// COMMAND ----------

import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.functions._

// COMMAND ----------

val silverNETSS = new SilverNETSS(spark)

// COMMAND ----------

val positionLookupCSV = spark.read.option("header", "true").csv(Env.netssPositionCSV)

val pivoted = positionLookupCSV.filter($"Position Start".isNotNull).withColumn("positions", array($"Position Start", $"Position End")).groupBy().pivot($"Column Name").agg(collect_set($"positions"))

display(pivoted)



// COMMAND ----------

val netssCases = spark.read.format("delta").load(Env.deltaNETSSBronzeDebatchedPath)

val cases = netssCases.select("case")

val joined = pivoted.join(cases)

def getValue = udf((range: Array[Array[String]], codedCase: String) => {
  val subArr = range(0)
  val start = subArr(0).toInt - 1
  val end = subArr(1).toInt
  codedCase.substring(start, end)
})
val selected = joined.select(
  getValue($"record_type", $"case").as("record_type"),
  getValue($"update", $"case").as("update"),
  getValue($"state", $"case").as("state"),
  getValue($"year", $"case").as("year"),
  getValue($"case_report_id", $"case").as("case_report_id"),
  getValue($"site_code", $"case").as("site_code"),
  getValue($"week", $"case").as("week"),
  getValue($"event_or_diagnosis", $"case").as("event_or_diagnosis"),
  getValue($"count", $"case").as("count"),
  getValue($"county", $"case").as("county"),
  getValue($"date_of_birth", $"case").as("date_of_birth"),
  getValue($"age", $"case").as("age"),
  getValue($"age_type", $"case").as("age_type"),
  getValue($"sex", $"case").as("sex"),
  getValue($"race", $"case").as("race"),
  getValue($"hispanic", $"case").as("hispanic"),
  getValue($"event_date", $"case").as("event_date"),
  getValue($"datetype", $"case").as("datetype"),
  getValue($"case_status", $"case").as("case_status"),
  getValue($"imported", $"case").as("imported"),
  getValue($"outbreak", $"case").as("outbreak"),
)

display(selected)


// COMMAND ----------

val valueLookupCSV = spark.read.option("header", "true").csv(Env.netssLookupCSV)
val recordTypeId = 1
val ageTypeId = 42
val sexId = 43
val raceId = 44
val hispanicId = 45

val recordTypeDF = valueLookupCSV.filter($"Start Position" === recordTypeId)
val ageTypeDF = valueLookupCSV.filter($"Start Position" === ageTypeId)
val sexDF = valueLookupCSV.filter($"Start Position" === sexId)
val raceDF = valueLookupCSV.filter($"Start Position" === raceId)
val hispanicDF = valueLookupCSV.filter($"Start Position" === hispanicId)

val lkjoin = selected.join(recordTypeDF, $"record_type" === recordTypeDF("Value")).drop("record_type", "Value", "Start Position").withColumnRenamed("Definition", "record_type")
                      .join(ageTypeDF, $"age_type" === ageTypeDF("Value")).drop("age_type", "Value", "Start Position").withColumnRenamed("Definition","age_type")
                      .join(sexDF, $"sex" === sexDF("Value")).drop("sex", "Value", "Start Position").withColumnRenamed("Definition","sex")
                      .join(raceDF, $"race" === raceDF("Value")).drop("race", "Value", "Start Position").withColumnRenamed("Definition", "race")
                      .join(hispanicDF, $"hispanic" === hispanicDF("Value")).drop("hispanic", "Value", "Start Position").withColumnRenamed("Definition", "hispanic")

val withCaseID = lkjoin.withColumn("unique_case_id", concat($"case_report_id", $"state", $"site_code", $"year"))
display(withCaseID)

// COMMAND ----------

val valueLookupCSV = spark.read.option("header", "true").csv(Env.netssLookupCSV_V2)
val recordTypeId = "record_type"
val ageTypeId = "age_type"
val sexId = "sex"
val raceId = "race"
val hispanicId = "hispanic"

val recordTypeDF = valueLookupCSV.filter($"column_name" === recordTypeId)
val ageTypeDF = valueLookupCSV.filter($"column_name" === ageTypeId)
val sexDF = valueLookupCSV.filter($"column_name" === sexId)
val raceDF = valueLookupCSV.filter($"column_name" === raceId)
val hispanicDF = valueLookupCSV.filter($"column_name" === hispanicId)

val lkjoin = selected.join(recordTypeDF, $"record_type" === recordTypeDF("value")).drop("record_type", "value", "column_name", "code", "code_system").withColumnRenamed("unit", "record_type")
                      .join(ageTypeDF, $"age_type" === ageTypeDF("value")).drop("age_type", "value", "column_name").withColumn("age_type",array($"unit", $"code", $"code_system"))
                        .drop("unit", "code", "code_system")
                      .join(sexDF, $"sex" === sexDF("value")).drop("sex", "value", "column_name", "code", "code_system").withColumnRenamed("unit","sex")
                      .join(raceDF, $"race" === raceDF("value")).drop("race", "value", "column_name").withColumn("race",array($"unit", $"code", $"code_system"))
                        .drop("unit", "code", "code_system")
                      .join(hispanicDF, $"hispanic" === hispanicDF("value")).drop("hispanic", "value", "column_name").withColumn("ethnicity",array($"unit", $"code", $"code_system"))
                        .drop("unit", "code", "code_system")

val withCaseID = lkjoin.withColumn("unique_case_id", concat($"case_report_id", $"state", $"site_code", $"year"))
display(withCaseID)
