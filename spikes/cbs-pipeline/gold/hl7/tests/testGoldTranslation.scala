// Databricks notebook source
// MAGIC %run ../fn_obxsEpi_to_cdm

// COMMAND ----------

val hl7Message = """Add sample HL7"""


// COMMAND ----------

// MAGIC %run ../../../silver/hl7/fn_hl7_to_obxs_and_segm

// COMMAND ----------

// // example use of obxsEpiToCDM

val ( obxsEpi, obxsNonEpi, otherSegments ) = HL7ToObxsAndSegments(hl7Message)

val mmgMap = MmgLoader.getAllMMGsAsMap("davt_xlr_cbsp_dev")

val ( obxsEpiInMmg, obxsEpiNotInMmg, mshDataElemMap, pidDataElemMap, obrDataElemMap, obxDataElemMapSingles, obxDataElemRepBlks ) = obxsEpiToCDM( obxsEpi, mmgMap("COVID19_MMG_V1.0"))

//println( "obxsEpiInMmg: --> " + obxsEpiInMmg )
//println( "obxsEpiNotInMmg: --> " + obxsEpiNotInMmg )
println( "obxsEpiInMmg.length: --> " + obxsEpiInMmg.length )
println( "obxsEpiNotInMmg.length: --> " + obxsEpiNotInMmg.length )

println( "mshDataElemMap: --> " + mshDataElemMap)
println( "pidDataElemMap: --> " + pidDataElemMap)
println( "obrDataElemMap: --> " + obrDataElemMap)
println( "obxDataElemMapSingles: --> " + obxDataElemMapSingles)
println( "obxDataElemRepBlks: --> " + obxDataElemRepBlks)

// obxDataElemMapSingles.foreach( el => println(s"--> $el"))

// COMMAND ----------


