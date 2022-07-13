// Databricks notebook source
// MAGIC %run "../common/config"

// COMMAND ----------

// Read the MMG Table
//println("deltaLakeMMGTablePath: " + Env.deltaLakeMMGTablePath)

val mmgTable = spark.read
            .format("delta")
            .load(XLRLake.getTablePath(XLRLake.MMGTable))

// display(mmgTable)

// COMMAND ----------

import scala.util.{Try,Success,Failure}

val mmgProfileIdentifierList = mmgTable.select("profileIdentifier").distinct().map( el => el.getString(0) ).collect().toList

// for each profile found:
mmgProfileIdentifierList.map(oneProfile => {
  
  val hl7Gold = Try( spark.read
                          .format("delta")
                          .table(XLRLake.getTableRefByMMG(XLRLake.goldHL7, oneProfile)) ).getOrElse( spark.emptyDataFrame )
                          //.load( Env.Hl7.deltaLakeGoldPath + oneProfile ) ).getOrElse( spark.emptyDataFrame )
  

  val csvGold = Try( spark.read
                          .format("delta")
                          .table(XLRLake.getTableRefByMMG(XLRLake.CSVGoldTable, oneProfile)) ).getOrElse( spark.emptyDataFrame )
                          //.load( Env.Csv.deltaLakeGoldPath + oneProfile ) ).getOrElse( spark.emptyDataFrame )
    
    
    
    val merged = (hl7Gold.count, csvGold.count) match {
      
      case (0, 0) => spark.emptyDataFrame
      case (0, _) => csvGold
      case (_, 0) => hl7Gold
      case (_, _) => hl7Gold.union(csvGold)
      
    } // .match
    
    
    if ( merged.count > 0 ) {
      // there is data to write
      
      // Write cdm to gold
      println( "writing to CDM profile --> " + oneProfile + ", hl7Gold.count --> " + hl7Gold.count + ", csvGold.count --> " + csvGold.count )
      merged.write.format("delta")
//         .option("path", Env.deltaLakeGoldPath + oneProfile + "_test") 
        .option("mergeSchema", "true")
        .mode("overwrite")
        .saveAsTable(XLRLake.getTableRefByMMG(XLRLake.goldCDM, oneProfile))
    
    } // .if 
    

}) // /. for each profile found

// COMMAND ----------


