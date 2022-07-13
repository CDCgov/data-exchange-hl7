// Databricks notebook source
// MAGIC %run ../../common/adfParams

// COMMAND ----------

// MAGIC %run ../../mmg/mmg_provider

// COMMAND ----------

val mmgMap = new MmgProvider(new MmgLoader).getAllMap

val profilesAvailable = mmgMap.keySet
println($"profilesAvailable: -> $profilesAvailable\n")

// COMMAND ----------

val allData = spark.read.format("delta").table( Lake.getTableRef(dbName, goldWithContValRep) )
display( allData.groupBy($"__message_info.profile_identifier").count )

// COMMAND ----------

allData.columns.size

// COMMAND ----------

val allMmgSeq  = mmgMap.map { case (key, value) => value }.flatten // only unique vals

// mmgSeq
// 0 profileID, 1 blkType, 2 blkName, 3 blkOrdinal, 4  blkID, 
// 5 elemName,  6 elemOrdinal, 7 elemDataType, 8 elemIsRepeat, 9  elemValueSetCode, 
// 10 elemSetVersionNumber, 11 elemIdentifier, 12 elemSegmentType, 13 elemFieldPosition, 14 elemComponentPosition, 
// 15 elemCardinality, 16 hl7v2DataType, 17 - codeSystem, 18 blkNameStd, 19 elemNameStd 
val (singlesMmgLines, repeatsMmgLines) = allMmgSeq.partition( ml => ml(1) == "Single")

val singlesMmgLinesSeq = singlesMmgLines.toSeq
val repeatsMmgLinesSeq = repeatsMmgLines.toSeq

val elementNames = singlesMmgLinesSeq.map(ml => ml(19)).toSet.toSeq // only unique names
val blockNames = repeatsMmgLinesSeq.map(ml => ml(18)).toSet.toSeq // only unique names
 
println(elementNames.length, blockNames.length, elementNames.length + blockNames.length) 

val metaCols = List("__metadata", "__message_info", "mmwr_year_partition", "mmg_seq", "value_set_codes", "vocab_entries_map", "content_validation_report")
println( metaCols.length, elementNames.length + blockNames.length + metaCols.length )

// COMMAND ----------

display( allData )

// COMMAND ----------

val profile = "Generic_MMG_V2.0"

val dfOneProfile = allData.filter($"__message_info.profile_identifier" === profile)

display( dfOneProfile )

// COMMAND ----------

dfOneProfile.columns.size

// COMMAND ----------

val mmgSeq = mmgMap(profile)

// pick up needed columns for this mmg
val cols = mmgSeq.map( ml => {
  ml(1) match {
    case "Single" => ml(19) // -> element name
    case _ => ml(18) // -> block name
  } // .match
}).toSet.toSeq.sorted

println( cols.length, cols )

// COMMAND ----------

// dataframe columns: metadata + mmg columns
val allCols = metaCols ++ cols

// COMMAND ----------

val oneData = dfOneProfile.select( allCols.map(c => col(c)):_* )

display(oneData)

// COMMAND ----------


