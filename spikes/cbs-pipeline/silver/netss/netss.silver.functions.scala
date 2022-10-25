// Databricks notebook source
// MAGIC %run "../../common/config"

// COMMAND ----------

import org.apache.spark.sql.{Row, DataFrame, SparkSession}
import org.apache.spark.sql.types.{ArrayType, StringType, StructType}
import org.apache.spark.sql.functions._

class SilverNETSS(sprk: SparkSession) {
  import sprk.sqlContext.implicits._
  def getColumnPositions(df: DataFrame): DataFrame = {
    // receives the CSV dataframe of the positions lookup 
    return df
  }
}
