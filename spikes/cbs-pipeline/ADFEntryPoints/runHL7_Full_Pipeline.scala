// Databricks notebook source
val startTime = System.currentTimeMillis

// COMMAND ----------

// MAGIC %run ./runHL7BronzePipelineFull

// COMMAND ----------

val bronzeDuration = ( System.currentTimeMillis - startTime ) / ( 1000 * 60 ).toFloat
println( "runHL7BronzePipelineFull Duration: " + bronzeDuration + " minutes")
val startTimeSilver = System.currentTimeMillis

// COMMAND ----------

// MAGIC %run ./runHL7SilverPipelineFull

// COMMAND ----------

val silverDuration = ( System.currentTimeMillis - startTimeSilver ) / ( 1000 * 60 ).toFloat
println( "runHL7SilverPipelineFull Duration: " + silverDuration + " minutes")
val startTimeGold = System.currentTimeMillis

// COMMAND ----------

// MAGIC %run ./runHL7GoldPipelineFull

// COMMAND ----------

val goldDuration =  ( System.currentTimeMillis - startTimeGold ) / ( 1000 * 60 ).toFloat
println( "runHL7GoldPipelineFull Duration: " + goldDuration + " minutes")
val startTimeGoldCVR = System.currentTimeMillis

// COMMAND ----------

// MAGIC %run ./runHL7GoldContentValidation

// COMMAND ----------

val goldContentValidationDuration = ( System.currentTimeMillis - startTimeGoldCVR ) / ( 1000 * 60 ).toFloat 
println( "runHL7GoldContentValidation Duration: " + goldContentValidationDuration + " minutes")

// COMMAND ----------

val finalTime = System.currentTimeMillis

// COMMAND ----------

val totalDuration = ( finalTime - startTime )

println("ingressTableName: --> " + ingressTableName)
println("Bronze Duration:                   " + bronzeDuration)
println("Silver Duration:                   " + silverDuration)
println("Gold Duration:                     " + goldDuration)
println("Gold Content Validation Duration:  " + goldContentValidationDuration)
println( "HL7 Full Pipeline Total Duration: " + totalDuration/ ( 1000 * 60 ).toFloat + " minutes")

// COMMAND ----------

// CLUSTER: 1 driver + 10 workers of: Standard_DS5_v2 56GB 16 Cores
// Standard_DS5_v2 DBU / hour: 33


// ingressTableName: --> cbs_bronze_hl7_dev1_raw_test_1k
// Bronze Duration:                   0.26608333
// Silver Duration:                   0.14553334
// Gold Duration:                     0.47125
// Gold Content Validation Duration:  1.0323
// HL7 Full Pipeline Total Duration: 1.9184667 minutes


// ingressTableName: --> cbs_bronze_hl7_dev1_raw_test_10k
// Bronze Duration:                   0.29648334
// Silver Duration:                   0.1408
// Gold Duration:                     0.5051
// Gold Content Validation Duration:  1.0658667
// HL7 Full Pipeline Total Duration: 2.0115666 minutes


// ingressTableName: --> cbs_bronze_hl7_dev1_raw_test_100k
// Bronze Duration:                   0.79316664
// Silver Duration:                   0.17905
// Gold Duration:                     0.71316665
// Gold Content Validation Duration:  1.5939333
// HL7 Full Pipeline Total Duration: 3.2848833 minutes
// totalDuration: Long = 197093


// ingressTableName: --> cbs_bronze_hl7_dev1_raw_test_1mil
// Bronze Duration:                   3.2194166
// Silver Duration:                   0.22403333
// Gold Duration:                     1.13355
// Gold Content Validation Duration:  3.9917333
// HL7 Full Pipeline Total Duration: 8.572083 minutes
