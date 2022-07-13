// Databricks notebook source
// MAGIC %run ../common/adfParams

// COMMAND ----------

// MAGIC %run ../gold/hl7/fn_obxsEpi_to_cdm

// COMMAND ----------

val df1 = Lake.readDf(dbName, silverOBXLakeAndSegments, isPipelineStreaming)
          .select("__metadata", "__message_info", "obx_epi")



// COMMAND ----------

// MAGIC %run ../mmg/mmg_provider

// COMMAND ----------

// map of profile -> mmg lines
val mmgMap = new MmgProvider(new MmgLoader).getAllMap

val profilesAvailable = mmgMap.keySet
//println($"profilesAvailable: -> $profilesAvailable")


// COMMAND ----------

val addMmgSeq = udf ( ( profileIdentifier: String )  => {
  
  profilesAvailable.contains( profileIdentifier ) match {
    case true => mmgMap(profileIdentifier)
    case _ => null
  } // .profilesAvailable
  
}) // .addMmgSeq

// COMMAND ----------

// filter out messages with profiles not available, these messages would have to be re-uploaded once the profile is available in this strategy

val df2 = df1.withColumn("mmg_seq", addMmgSeq( $"__message_info.profile_identifier") )
              .filter( $"mmg_seq".isNotNull ) // only messages that have a valid and matching profileIdentifier
              

// COMMAND ----------

val toCDM = udf ( ( obxsEpi: Seq[(String, String, String, String)], mmgSeq: Seq[Seq[String]] )  => {

  obxsEpiToCDM(obxsEpi, mmgSeq)
  
}) // .toCDM


// COMMAND ----------

val df3 = df2.withColumn("obxsEpiToCDM", toCDM( $"obx_epi", $"mmg_seq") ).select("__metadata", "__message_info", "obx_epi", "obxsEpiToCDM.*", "mmg_seq")
              .withColumnRenamed("_1", "obx_epi_in_mmg") //  obxsEpiInMmg, obxsEpiNotInMmg, mshDataElemMap, pidDataElemMap, obrDataElemMap, obxDataElemMapSingles, obxDataElemRepBlks 
              .withColumnRenamed("_2", "obx_epi_notinmmg") 
              .withColumnRenamed("_3", "msh_data_elem_map")
              .withColumnRenamed("_4", "pid_data_elem_map")
              .withColumnRenamed("_5", "obr_data_elem_map")
              .withColumnRenamed("_6", "obx_data_elem_map_singles")
              .withColumnRenamed("_7", "obx_data_elem_rep_blks")

//display( df3)

// COMMAND ----------

val df4 = df3.select( "__metadata", "__message_info", "msh_data_elem_map", "pid_data_elem_map", "obr_data_elem_map", "obx_data_elem_map_singles", "obx_data_elem_rep_blks", "mmg_seq")

//display ( df4 )

// COMMAND ----------

// cumulate from the mmg df all the elements and all the blocks for foldLeft
//println( mmgMap )

val allMmgSeq  = mmgMap.map { case (key, value) => value }.flatten // only unique vals

// mmgSeq
// 0 profileID, 1 blkType, 2 blkName, 3 blkOrdinal, 4  blkID, 
// 5 elemName,  6 elemOrdinal, 7 elemDataType, 8 elemIsRepeat, 9  elemValueSetCode, 
// 10 elemSetVersionNumber, 11 elemIdentifier, 12 elemSegmentType, 13 elemFieldPosition, 14 elemComponentPosition, 
// 15 elemCardinality, 16 hl7v2DataType, 17 - codeSystem, 18 blkNameStd, 19 elemNameStd 

val (singlesMmgLines, repeatsMmgLines) = allMmgSeq.partition( ml => ml(1) == "Single")

val singlesMmgLinesSeq = singlesMmgLines.toSeq
val repeatsMmgLinesSeq = repeatsMmgLines.toSeq

//println("all mmg lines --> singles, repeats, single+repeats: ", singlesMmgLinesSeq.length, repeatsMmgLinesSeq.length,  singlesMmgLinesSeq.length + repeatsMmgLinesSeq.length )

val elementNames = singlesMmgLinesSeq.map(ml => ml(19)).toSet.toSeq // only unique names
val blockNames = repeatsMmgLinesSeq.map(ml => ml(18)).toSet.toSeq // only unique names

//println("all mmg lines --> unique elementNames.length: --> " + elementNames.length)
//println("all mmg lines --> unique blockNames.length: --> " + blockNames.length)

val elementSegTups = elementNames.map( elName => {
  val mmgLine = singlesMmgLines.filter( ml => ml(19) == elName ).toSeq(0)
  
  (mmgLine(12), elName)
}) // .elementSegTups


val allCodeSystems = allMmgSeq.filter( ml => ml(7) == "Coded").map( ml => ml(9)).toSet.toSeq.filter(_ != "N/A") // only available and once, unique
//println("allCodeSystems: --> " + allCodeSystems)

// COMMAND ----------

// Expanding "msh_data_elem_map", "pid_data_elem_map", "obr_data_elem_map", "obx_data_elem_map_singles" into columns for mmg element

val extractValueT2 = udf ( (elemName: String, mp: Map[String, Tuple2[String, String]]) => {
  
  mp.contains(elemName) match {
    case true => mp(elemName)._2 // picking up the value only for the respective element 
    case _ => null //"" // not available in the message 
  } // .mp contains
  
}) // .extractValueT2

val extractValueT4 = udf ( (elemName: String, mp: Map[String, Tuple4[String, String, String, String]]) => {
  
  mp.contains(elemName) match {
    case true => {
      
        val mapEntry = mp(elemName)
        (mapEntry._2, mapEntry._4)  // (obxDataType, obxData) is selected for singles
      
    } // case true
    
    case _ => null//("", "", "") // not available in the message 
  } // .mp contains
  

}) // .extractValueT4

val df5 = elementSegTups.foldLeft(df4) { (acc, element) => {
  
  val elementSegment = element._1
  val elementName = element._2
  
  val column = elementSegment match {
    case "MSH" => extractValueT2( lit(elementName), $"msh_data_elem_map" )
    case "PID" => extractValueT2( lit(elementName), $"pid_data_elem_map" )
    
    case "OBR" => extractValueT2( lit(elementName), $"obr_data_elem_map" )
    case _ => extractValueT4( lit(elementName), $"obx_data_elem_map_singles" )
  } // .elementSegment match
  
  // acc.withColumn(elementSegment + "_" + elementName, column)
  acc.withColumn( elementName, column )
  
}}.drop("msh_data_elem_map", "pid_data_elem_map", "obr_data_elem_map", "obx_data_elem_map_singles")


//display( df5 )

// COMMAND ----------

// Expanding "obx_data_elem_rep_blks" into columns for mmg block

val extractValueBlock = udf ( (blockName: String, mp: Map[String,Seq[Map[String,(String, String)]]]) => {
  
  mp.contains(blockName) match {
    case true => mp(blockName) // (obxDataType, obxData) already there for repeats
    
    case _ => null // not available in the message 
  } // .mp contains

}) // .extractValueBlock

val df6 = blockNames.foldLeft(df5) { (acc, block) => {
  
  val blockName = block
  
  val column = extractValueBlock( lit(blockName), $"obx_data_elem_rep_blks" )
  
//   acc.withColumn("OBX_" + blockName, column)
  acc.withColumn( blockName, column )
  
}}.drop("obx_data_elem_rep_blks")


//display( df6 )

// COMMAND ----------

// add columns for each message from mmg: for elements and blocks needed

// mmgSeq
// 0 profileID, 1 blkType, 2 blkName, 3 blkOrdinal, 4  blkID, 
// 5 elemName,  6 elemOrdinal, 7 elemDataType, 8 elemIsRepeat, 9  elemValueSetCode, 
// 10 elemSetVersionNumber, 11 elemIdentifier, 12 elemSegmentType, 13 elemFieldPosition, 14 elemComponentPosition, 
// 15 elemCardinality, 16 hl7v2DataType, 17 - codeSystem, 18 blkNameStd, 19 elemNameStd 

val partitionMmgSeq = udf ( (mmgSeq: List[List[String]]) => {
  
  val ( elementsMl, blocksMl ) = mmgSeq.partition(ml => ml(1) == "Single") 

  val elements = elementsMl.map(ml => ml(19) ).toSet.toSeq
  val blocks = blocksMl.map(ml => ml(18)).toSet.toSeq // only unique names
  
  (elements, blocks)
}) // .partitionMmgSeq

val df7 = df6.withColumn("mmg_seq_tup", partitionMmgSeq($"mmg_seq"))
             .withColumn("mmg_seq_singles", $"mmg_seq_tup._1").withColumn("mmg_seq_repeats", $"mmg_seq_tup._2")
             .drop("mmg_seq_tup")

//display(df7)

// COMMAND ----------

// mmgSeq
// 0 profileID, 1 blkType, 2 blkName, 3 blkOrdinal, 4  blkID, 
// 5 elemName,  6 elemOrdinal, 7 elemDataType, 8 elemIsRepeat, 9  elemValueSetCode, 
// 10 elemSetVersionNumber, 11 elemIdentifier, 12 elemSegmentType, 13 elemFieldPosition, 14 elemComponentPosition, 
// 15 elemCardinality, 16 hl7v2DataType, 17 - codeSystem, 18 blkNameStd, 19 elemNameStd 

val addMmgValueSetCodes = udf ( ( mmgSeq: Seq[Seq[String]] )  => {
  
  mmgSeq.filter( ml => ml(7) == "Coded").map( ml => ml(9)).toSet.toSeq.filter(_ != "N/A") // only available and once, unique
  
}) // .addMmgValueSetCodes

// this will be needed at content validation
val df8 = df7.withColumn("value_set_codes", addMmgValueSetCodes($"mmg_seq"))

//display( df8 )

// COMMAND ----------

// creating partitions per MMWR YEAR
// other ideas: per year, month, day 
// or per event code


// partition per MMWR_YEAR

// val df9 = df8.withColumn("mmwr_year_partition", $"mmwr_year._2")
val df9 = df8.withColumn("mmwr_year_partition", when($"mmwr_year".isNotNull, $"mmwr_year._2").otherwise("not_available"))
//display( df9 )

// COMMAND ----------

// sort columns 
val metaCols = List("__metadata", "__message_info", "mmg_seq", "mmg_seq_singles", "mmg_seq_repeats", "value_set_codes", "mmwr_year_partition")

val sortedCols = df9.columns.filter(!metaCols.contains(_)).sortWith(_ < _)

val allCols = metaCols ++ sortedCols.toList

val df10 = df9.select(allCols.map(el=>col(el)):_*)

//display( df10 )

// COMMAND ----------

Lake.writeDf(df9, dbName, goldTable, mntDelta, "mmwr_year_partition", isPipelineStreaming)
// def writeDf(df: DataFrame, dbName: String, tableName: String, mntDelta: String, partitionBy: String, isStreaming: Boolean)


// COMMAND ----------


