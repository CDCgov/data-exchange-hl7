// Databricks notebook source
val startTime = System.currentTimeMillis

// COMMAND ----------

// MAGIC %run ../common/adfParams

// COMMAND ----------

val df0 = Lake.readDf(dbName, "cbs_gold_hl7_cvr_dev1", false)

// val df1t = df0.union(df0).union(df0).union(df0).union(df0).union(df0).union(df0).union(df0).union(df0).union(df0)
// val df1 = df1t.union(df1t)

val df1 = df0

df1.count

// COMMAND ----------

df1.rdd.getNumPartitions

// COMMAND ----------

// by case id, Obr22 - takes precedence, Msh7, or last the timestamp of the message from spark ingestion(__meta_ingestTimestamp), or lastest timestamp from storage bucket 

// OBR-22 -> date_of_electronic_case_notification_to_cdc
// MSH-7 -> __message_info.msh7
// __metadata.__meta_ingest_timestamp
// __metadata.__meta_file_timestamp
// TODO: add last resort separator by message uuid for 2 perfect identical messages

display( df1 ) // .select( "date_of_electronic_case_notification_to_cdc", "__message_info.msh7", "__metadata.__meta_ingest_timestamp", "__metadata.__meta_file_timestamp" )

// COMMAND ----------

import org.apache.spark.sql.functions._

// TODO: check setting
spark.conf.set("spark.sql.legacy.timeParserPolicy","LEGACY")

val df2 = df1.withColumn( "obr22_epoch",  unix_timestamp( $"date_of_electronic_case_notification_to_cdc", "yyyyMMddHHmmss" ) )
             .withColumn( "msh7_epoch",  unix_timestamp( $"__message_info.msh7", "yyyyMMddHHmmss" ) )

display( df2 ) // .select("obr22_epoch", "msh7_epoch")

// COMMAND ----------

val df3 = df2.groupBy($"__message_info.__anly_case_id")
// val df3 = df2.groupBy($"__message_info.__anly_case_id", $"date_of_electronic_case_notification_to_cdc", $"__message_info.msh7", $"__metadata.__meta_ingest_timestamp", $"__metadata.__meta_file_timestamp") // this groupBy will eliminate any complete duplicates
 .agg( count("__metadata").as("message_count"), 
       sort_array ( collect_list( $"obr22_epoch" ), true ).as("obr22_epoch_case_id_list" ), 
       sort_array ( collect_list( $"msh7_epoch" ), true ).as("msh7_epoch_case_id_list" ) ,
       sort_array ( collect_list( $"__metadata.__meta_ingest_timestamp" ), true ).as("__meta_ingest_timestamp_list" ),
       sort_array ( collect_list( $"__metadata.__meta_file_timestamp" ), true ).as("__meta_file_timestamp_list" ) 
      // TODO: message uuid + message position in the batch
     ) // .agg
  .withColumnRenamed("date_of_electronic_case_notification_to_cdc", "date_of_electronic_case_notification_to_cdc_")

display( df3 )

// COMMAND ----------

df3.rdd.getNumPartitions

// COMMAND ----------

val df4 = df2.join(df3, $"__message_info.__anly_case_id" === $"__anly_case_id", "inner")
              .drop("__anly_case_id", "date_of_electronic_case_notification_to_cdc_", "msh7", "__meta_ingest_timestamp", "__meta_file_timestamp") 

display( df4 ) // .sort($"message_count".desc

// COMMAND ----------

import java.sql.Timestamp

val DUPE = ("not_solvable_dupe", -1)

def checkOrder( t: Timestamp, tl: List[Timestamp]) = {
  
    tl.count( _ == t) match {
        case 1 => { // there is only one occurence of this time in the list

            ( t == tl.last) match {

              case true => ("true", tl.size)  // this is the latest message
              
              case false => ("false", tl.indexOf(t) + 1) // not the latest, order at position
              
            } // .match

        } // .1

        case _ => DUPE // can not be ordered, is a duplicate
      }
} // .checkOrder

// COMMAND ----------

val flagMessage = udf( (obr22: Timestamp, msh7: Timestamp, ingest: Timestamp, file: Timestamp, obr22List: List[Timestamp], msh7List: List[Timestamp], ingestList: List[Timestamp], fileList: List[Timestamp]) => {
  
  // check to see if possible to match by obr22 or is this a duplicate obr22
  val orderObr22 = checkOrder(obr22, obr22List) 
  
  orderObr22 match {
    case DUPE => {
      
          // check to see if possible to match by msh7
          val orderMsh7 = checkOrder(msh7, msh7List)

          orderMsh7 match {
            case DUPE => {
               // check to see if possible to match by file ingest 
              val orderIngest = checkOrder(ingest, ingestList) 
              
              orderIngest match {
                case DUPE => {
                  // check order file modify
                        val orderFile = checkOrder(file, fileList) 

                        orderFile match {
                          case DUPE => DUPE // complete duplicate by all checks, this should not happen 
                                            // TODO: pick latest by message uuid
                          
                          case _ => orderFile
                        } // .orderIngest match
                  
                } // .DUPE orderIngest
                case _ => orderIngest
              } // .orderIngest match
              
              
            } // .DUPE msh7
            
            case _ => orderMsh7 // order by msh7 ok
          } // .orderMsh7
      
    } // .DUPE obr22
    case _ => orderObr22 // order by obr22 ok
  } // .orderObr22
  
}) // .flagMessage

// COMMAND ----------

val df5 = df4.withColumn("flag_message", 
                         flagMessage(
                           $"obr22_epoch", $"msh7_epoch", $"__metadata.__meta_ingest_timestamp", $"__metadata.__meta_file_timestamp", $"obr22_epoch_case_id_list", $"msh7_epoch_case_id_list", $"__meta_ingest_timestamp_list", $"__meta_file_timestamp_list")
                        )
              .select("*", "flag_message.*")
              .withColumnRenamed("_1", "last_flag")
              .withColumnRenamed("_2", "message_order")

display( df5.filter($"__message_info.__anly_case_id" === "Malaria_Sample06~~13")) //.groupBy("__message_info.__anly_case_id").count ) // .select("last_flag", "message_order")

// COMMAND ----------

// isPipelineStreaming to false, this is a batch job to sequence all messages

Lake.writeDf(df5, dbName, goldWithFlag, mntDelta, "mmwr_year_partition", false)

// COMMAND ----------

val finalTime = System.currentTimeMillis

// COMMAND ----------

val totalDuration = ( finalTime - startTime )
println( "df5.count: --> " + df5.count )
println( "latest_flag_and_sequencing duration: " + totalDuration/ ( 1000 * 60 ).toFloat + " minutes")


// COMMAND ----------

// CLUSTER: 1 driver + 10 workers of: Standard_DS5_v2 56GB 16 Cores
// Standard_DS5_v2 DBU / hour: 33

// df5.count: --> 998,154
// latest_flag_and_sequencing duration: 2.0704 minutes

// df5.count: --> 998154
// latest_flag_and_sequencing duration: 2.8186 minutes

// df5.count: --> 998,154
// latest_flag_and_sequencing duration: 3.05295 minutes

// df5.count: --> 1,996,308
// latest_flag_and_sequencing duration: 3.6350834 minutes

// df5.count: --> 2,994,462
// latest_flag_and_sequencing duration: 4.8512335 minutes

// df5.count: --> 3,992,616
// latest_flag_and_sequencing duration: 5.0858335 minutes

// df5.count: --> 4990770
// latest_flag_and_sequencing duration: 6.38875 minutes

// df5.count: --> 9,981,540
// latest_flag_and_sequencing duration: 12.397917 minutes

// df5.count: --> 19,963,080
// latest_flag_and_sequencing duration: 33.513115 minutes
