// Databricks notebook source
trait LakeConfig {
  def getTablePath(deltaRoot: String, dbName: String, tableName: String): String
  def getTableRef(dbName: String, tableName: String): String
  def getCheckpointLocation(deltaRoot: String, dbName: String, tableName: String): String
}

import org.apache.spark.sql.{DataFrame }
import org.apache.spark.sql.streaming.Trigger

object Lake {
  
    def getTablePath(deltaRoot: String, dbName: String, tableName: String): String = {
       deltaRoot + "database/" + dbName + "/" + tableName 
    }
    def getTableRef(dbName: String, tableName: String): String = {
       dbName + "." + tableName
    }

    def getCheckpointLocation(deltaRoot: String, dbName: String, tableName: String): String = {
       deltaRoot + "checkpoints/" + dbName + "/" + tableName 
    }


    // read and write lake functions


    def readDf(dbName: String, tableName: String, isStreaming: Boolean): DataFrame = {

      isStreaming match {
                                                                                           //current: CLUSTER: 1 driver + 10 workers of: Standard_DS5_v2 56GB 16 Cores
        case true => spark.readStream.format("delta").table( getTableRef(dbName, tableName) ).repartition(320) // The ideal number of partitions = total number of cores X 4. 

        case _ => spark.read.format("delta").table( getTableRef(dbName, tableName) ).repartition(320) // The ideal number of partitions = total number of cores X 4. 

      } // .isStreaming

    } // .readDf
  
    
    def writeDf(df: DataFrame, dbName: String, tableName: String, mntDelta: String, partitionBy: String, isStreaming: Boolean) = {
      
      (partitionBy.equals(""), isStreaming) match {
        
          case (true, false) => df.write.format("delta").mode(SaveMode.Overwrite).saveAsTable( getTableRef(dbName, tableName) )
        
          case (false, false) => df.write.format("delta").partitionBy(partitionBy).mode(SaveMode.Overwrite).saveAsTable( getTableRef(dbName, tableName) )
          // todo
          case (true, true) =>  df.writeStream.format("delta")
                                  .option("checkpointLocation", getCheckpointLocation( mntDelta, dbName, tableName) )
                                  .trigger(Trigger.Once()) 
                                  .table( getTableRef(dbName, tableName) )
                                  .awaitTermination()
        
          case (_, _) => df.writeStream.format("delta")
                                  .partitionBy(partitionBy)
                                  .option("checkpointLocation", getCheckpointLocation(mntDelta, dbName, tableName) )
                                  .trigger(Trigger.Once()) 
                                  .table( getTableRef(dbName, tableName) )
                                  .awaitTermination()
         
      } // .match
      
    } // .writeDf
  
    
    def writeRefDf(df: DataFrame, tableRef: String, checkpointLocation: String, partitionBy: String, isStreaming: Boolean) = {
      
      (partitionBy.equals(""), isStreaming) match {
        
          case (true, false) => df.write.format("delta").mode(SaveMode.Overwrite).saveAsTable(tableRef)
        
          case (false, false) => df.write.format("delta").partitionBy(partitionBy).mode(SaveMode.Overwrite).saveAsTable(tableRef)
          // todo
          case (true, true) =>  df.writeStream.format("delta")
                                  .option("checkpointLocation", checkpointLocation)
                                  .trigger(Trigger.Once()) 
                                  .table(tableRef)
                                  .awaitTermination()
        
          case (_, _) => df.writeStream.format("delta")
                                  .partitionBy(partitionBy)
                                  .option("checkpointLocation", checkpointLocation)
                                  .trigger(Trigger.Once()) 
                                  .table(tableRef)
                                  .awaitTermination()
         
      } // .match
      
    } // .writeRefDf
  
} // .Lake
