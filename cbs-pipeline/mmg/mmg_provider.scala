// Databricks notebook source
// MAGIC %run ./mmg_loader

// COMMAND ----------

class MmgProvider(mmgLdr: MmgLoader) {
  
  val MMG_ELEM_MISSING_LONG = -1
    
  def getAllMap(): Map[String,Seq[Seq[String]]] = {
    mmgLdr.getAllDf
      .select($"profileIdentifier", 
                       $"block_type", 
                       $"block_name", 
                       $"block_ordinal", 
                       $"block_id", 
                       $"element_name", 
                       $"element_ordinal", 
                       $"element_dataType", 
                       $"element_isRepeat", 
                       $"element_valueSetCode", 
                       $"element_valueSetVersionNumber",
                       $"element_identifier", 
                       $"element_segmentType",
                       $"element_fieldPosition", 
                       $"element_componentPosition", 
                       $"element_cardinality",
                       $"element_hl7v2DataType",
                       $"element_codeSystem",
                       $"block_name_common",
                       $"element_name_common")
    .na.fill("").na.fill(MMG_ELEM_MISSING_LONG)
    .collect().map(row => row.toSeq.map(_.toString) ).map( row => (row(0), row)).groupBy(_._1).map{ case(k,v) => k -> v.map(_._2).toSeq }
  } // .getAllMap
    
} // .MmgProvider



// COMMAND ----------


