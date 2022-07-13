// Databricks notebook source
import org.apache.spark.sql.{Row, DataFrame, SparkSession}
import org.apache.spark.sql.types.{ArrayType, StringType, StructType}
import org.apache.spark.sql.functions._

class BronzeNETSS(sprk: SparkSession) {
  import sprk.sqlContext.implicits._
  def debatch(df: DataFrame): DataFrame = {
    val carriageReturns = "\\\r\\\n|\\\n\\\r|\\\r|\\\n"
    df.withColumn("case", explode(split($"decoded", carriageReturns)))
      .filter($"case".startsWith("M"))
      .drop($"decoded")
      .drop($"content")
  }
}

// COMMAND ----------


