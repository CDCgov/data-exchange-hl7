// Databricks notebook source
import org.apache.spark.sql.{ Column }
import org.apache.spark.sql.functions._

object Funcs {
  
  // Used in .CSV for MMG block and elem names change

  // this is needed because MMG names are not valid DF column names
    def renameColHeader(c: Column):Column = {

      // TODO - improve, keep in sync

      val rr1 = trim( lower(c) )
      val rr2 = regexp_replace( rr1, "\\s", "_") 
      val rr3 = regexp_replace( rr2, "-", "_") 
      val rr4 = regexp_replace( rr3, "/", "")
      val rr5 = regexp_replace( rr4, "&", "and")
      val rr6 = regexp_replace( rr5, "(_)\\1+", "_")
      val rr7 = regexp_replace( rr6, "[^A-Z a-z 0-9 _]", "" )

      rr7      
    } // renameColHeader

  
} // .Funcs


// COMMAND ----------


