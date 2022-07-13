// Databricks notebook source
val df1 = sc.parallelize(1 to 1000000000)

// COMMAND ----------

println("df1.getNumPartitions: --> " + df1.getNumPartitions)
println("df1.count: --> " + df1.count)

// COMMAND ----------

println("df1.getNumPartitions: --> " + df1.getNumPartitions)
println("df1.sum: --> " + df1.sum)

// COMMAND ----------

val df2 = df1.coalesce(2)
println("df2.getNumPartitions: --> " + df2.getNumPartitions)

// COMMAND ----------

println("df2.getNumPartitions: --> " + df2.getNumPartitions)
println("df2.sum: --> " + df2.sum)

// COMMAND ----------


