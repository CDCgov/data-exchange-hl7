// Databricks notebook source
// MAGIC %run "../../common/all_configs"

// COMMAND ----------

// println("Env.Csv.testFile: " + Env.Csv.testFile) 

val csvData = spark.read.option("header", "true").csv(XLRLake.mntIngress + dbutils.widgets.get("fileToIngest") )
               .na.drop("all") // drop rows when all columns are null
               .withColumn("__meta_ingestTimestamp", current_timestamp)

display(csvData)

// COMMAND ----------

// select message profile identifier from the multi-column message_profile_identifiers, use message_profile_identifier as a fall-back only
val mpi1 = {
  
  // first choice would be the last column of the message_profile_identifiers
  val mpisArr = csvData.columns.filter(_.startsWith("message_profile_identifiers"))

  if ( mpisArr.size > 0 ) {

    val mpiCol = mpisArr.sortWith(_ > _).head
    csvData.select("`" + mpiCol + "`").first.getString(0) // this could be null

  } else {
    // second choice - select from unique column message_profile_identifier
    csvData.select("message_profile_identifier").first.getString(0)
  } // .if else
  
} // .messageProfileIdentifier1

var messageProfileIdentifier = ""
if (mpi1 == null) { // first choice could be null in the csv
  messageProfileIdentifier = csvData.select("message_profile_identifier").first.getString(0)
} // .if

println("messageProfileIdentifier for csv file: " + messageProfileIdentifier)

if ( messageProfileIdentifier.size == 0 ) {
  throw new RuntimeException("error extracting the messageProfileIdentifier for this message(csv file)")
} // .if 


// COMMAND ----------

// Read the MMG Table, filtered by file profile
val mmgTable = spark.read
            .format("delta")
            .table(XLRLake.getTableRef(XLRLake.MMGTable))
            .filter($"profileIdentifier" === messageProfileIdentifier)
            .withColumn("element_name_csv", Funcs.renameColHeader($"element_name") )
            .withColumn("block_name_csv",  Funcs.renameColHeader($"block_name") )
            .drop("element_identifier", "element_segmentType", "element_fieldPosition", "element_componentPosition", "element_cardinality", "__mmg_meta_ingestTimestamp", "__mmg_meta_ingestUuid")


if ( mmgTable.count == 0 ) {
  throw new RuntimeException("MMG Table empty, check MMG is available for this profile: " + messageProfileIdentifier)
} // .if

display(mmgTable)

// COMMAND ----------

// define some general constants

val EXTENSION_CODE = "__code"
val EXTENSION_CODE_SYSTEM = "__code_system"

// COMMAND ----------

val csvHeaderSet0 = csvData.columns.toSet

if ( csvHeaderSet0.size != csvData.columns.toList.size ) {
  throw new RuntimeException("CSV File has repeated columns with the same name")
} // .if

val colsMetaToKeep = Set("source_format", "datetime_of_message", "unique_case_id")

val csvHeaderSet1 = csvHeaderSet0.filterNot(colsMetaToKeep)

// COMMAND ----------

// 1. Elements ( $"block_type" === "Single" && $"element_dataType" =!= "Coded" && $"element_isRepeat" === false )
//
// --------------------------------------------------

val singleNonCodedNonRepeats = mmgTable.filter($"block_type" === BLOCK_TYPE_SINGLE && $"element_dataType" =!= ELEMENT_DATATYPE_CODED && $"element_isRepeat" === false)

val singleNonCodedNonRepeatsSet = singleNonCodedNonRepeats.select("element_name_csv").map( r => r.getString(0)).collect.toSet 

//
// take out singleNonCodedNonRepeatsSet from csvHeaderSet1, these columns stay as is
//
val csvHeaderSet2 = csvHeaderSet1.filterNot(singleNonCodedNonRepeatsSet)


// COMMAND ----------

// check for step 1
println(csvHeaderSet1.size, csvHeaderSet2.size, singleNonCodedNonRepeatsSet.size)
val checksum1 = csvHeaderSet1.size == csvHeaderSet2.size + singleNonCodedNonRepeatsSet.size
if ( !checksum1 ) {
  throw new RuntimeException("code ref: checksum1")
} // .if

// COMMAND ----------

// 2. Elements ( $"block_type" === "Single" && $"element_dataType" === "Coded" && $"element_isRepeat" === false )
//
// --------------------------------------------------

val singleCodedNonRepeats = mmgTable.filter($"block_type" === BLOCK_TYPE_SINGLE && $"element_dataType" === ELEMENT_DATATYPE_CODED && $"element_isRepeat" === false)

val singleCodedNonRepeatsSet = singleCodedNonRepeats.select("element_name_csv").map( r => r.getString(0)).collect.toSet 

// remaining csv header
val csvHeaderSet3 = csvHeaderSet2.filterNot(singleCodedNonRepeatsSet)

var df1 = csvData

singleCodedNonRepeatsSet.foreach( colName => {
  
  val colNameCode = colName + EXTENSION_CODE
  val colNameCodeSystem = colName + EXTENSION_CODE_SYSTEM
  
  // println(colName, colNameCode, colNameCodeSystem)
  
    // df1 = df1.withColumn(colName, array( col(colNameCode), col(colName), col(colNameCodeSystem) ) ) // TODO: change to case class
    df1 = df1.withColumn(colName, toDataCoded( col(colNameCode), col(colName), col(colNameCodeSystem)) ) //.as("itemData") // array( col(colNameCode), col(colName), col(colNameCodeSystem)
        .drop(colNameCode, colNameCodeSystem)
})


display(df1)

// COMMAND ----------

// check for step 2
println( csvHeaderSet2.size, csvHeaderSet3.size, singleCodedNonRepeatsSet.size )
val checkSum2 = csvHeaderSet2.size == csvHeaderSet3.size + singleCodedNonRepeatsSet.size
if ( !checkSum2 ) {
  throw new RuntimeException("code ref: checkSum2")
} // .if


// COMMAND ----------

// 3. Elements ( $"block_type" === "Single" && $"element_dataType" =!= "Coded" && $"element_isRepeat" === true )
//
// --------------------------------------------------

val singleNonCodedRepeats = mmgTable.filter($"block_type" === BLOCK_TYPE_SINGLE && $"element_dataType" =!= ELEMENT_DATATYPE_CODED && $"element_isRepeat" === true)

val singleNonCodedRepeatsSet = singleNonCodedRepeats.select("element_name_csv").map( r => r.getString(0)).collect.toSet 

println("singleNonCodedRepeatsSet: --> " + singleNonCodedRepeatsSet)

// COMMAND ----------

var sncrNumDrop = 0 // counts the columns dropped

var df2 = df1
var csvHeaderSet4 = csvHeaderSet3

singleNonCodedRepeatsSet.foreach(mmgHeader => {
  
   
  val oneSetOfCsvRepeats = csvHeaderSet3.filter( csvHeader => {
    
    val csvHeaderUpToBracket = csvHeader.split("\\[")(0)

     // val csvHeaderUpToBracket = csvHeader.substring(0, Math.min(csvHeader.length, mmgHeader.length))
    
      mmgHeader == csvHeaderUpToBracket
  }).toSeq
  
  // println("oneSetOfCsvRepeat --------> " + oneSetOfCsvRepeats)
  
  // merge repeats
  df2 = df2.withColumn( mmgHeader, array( oneSetOfCsvRepeats.map(c => col(c)):_* )  ) 
  
  if ( oneSetOfCsvRepeats.size > 1 ) {
    // this to avoid when repeats are named without [0], and there is only one
    df2.drop( oneSetOfCsvRepeats.map(c => c):_*  )
    sncrNumDrop += oneSetOfCsvRepeats.size - 1 // minus 1 to count for the one added as mmgHeader
  } // .if
  
  // remaining csv header
  csvHeaderSet4 = csvHeaderSet4.filterNot(oneSetOfCsvRepeats.toSet)
  
}) // .singleNonCodedRepeatsSet


//display(df2.select("message_profile_identifier"))

// COMMAND ----------

// check for step 3
println( csvHeaderSet3.size, csvHeaderSet4.size, singleNonCodedRepeatsSet.size, sncrNumDrop)
val checkSum3 = csvHeaderSet3.size == csvHeaderSet4.size + singleNonCodedRepeatsSet.size + sncrNumDrop
if ( !checkSum3 ) {
  throw new RuntimeException("code ref: checkSum3")
} // .if

// COMMAND ----------

// 4. Elements ( $"block_type" === "Single" && $"element_dataType" === "Coded" && $"element_isRepeat" === true )
//
// --------------------------------------------------

val singleCodedRepeats = mmgTable.filter($"block_type" === BLOCK_TYPE_SINGLE && $"element_dataType" === "Coded" && $"element_isRepeat" === true)

val singleCodedRepeatsSet = singleCodedRepeats.select("element_name_csv").map( r => r.getString(0)).collect.toSet 
println("singleCodedRepeatsSet: --> " + singleCodedRepeatsSet)

// COMMAND ----------

var df3 = df2
var csvHeaderSet5 = csvHeaderSet4

var scrFilterOut = 0

singleCodedRepeatsSet.foreach(mmgHeader => {
  
   
  val oneSetOfCsvRepeats = csvHeaderSet3.filter( csvHeader => {
    
    
    val csvHeaderUpToBracket = csvHeader.split("\\[")(0)
    
    //val csvHeaderUpToCode = csvHeader.split(EXTENSION_CODE)(0) // splits for both __code and __code_system
    
      if (mmgHeader == csvHeaderUpToBracket) {
        true
      } else {
        false 
        // check for __code && __code_system
        // mmgHeader == csvHeaderUpToCode 
      } // .if 
  }).toSeq
  
  if ( oneSetOfCsvRepeats.size == 3 ) {
    // there is only one group
    println("-oneSetOfCsvRepeats--only 1 group-->" + oneSetOfCsvRepeats)
    
    // add data, code, system
    
    val colName = oneSetOfCsvRepeats.filter( !_.contains(EXTENSION_CODE) )(0)
    //println("colName--> " + colName)
    
    val colNameCode = oneSetOfCsvRepeats.filter( w => w.contains(EXTENSION_CODE) && !w.contains(EXTENSION_CODE_SYSTEM) )(0) 
    //println("colNameCode--> " + colNameCode)
  
    val colNameCodeSystem = oneSetOfCsvRepeats.filter(_.contains(EXTENSION_CODE_SYSTEM))(0)
    //println("colNameCodeSystem--> " + colNameCodeSystem)
    
    //df3 = df3.withColumn( mmgHeader, array(  col(colNameCode), col(colName), col(colNameCodeSystem) )  ) 
     df3 = df3.withColumn(colName, toDataCoded( col("`" + colNameCode + "`"), col("`" + colName + "`"), col("`" + colNameCodeSystem + "`")) )
              .drop( colNameCode, colNameCodeSystem )
   
    // remaining csv header, filter out 3 that are taken care of, 1 colName replaced with array value and 2 code and code_system dropped
    csvHeaderSet5 = csvHeaderSet5.filter(el=> !( el == colName || el == colNameCode || el == colNameCodeSystem) )
    scrFilterOut += 3 
    
    // // this should not be needed colName should == mmgHeader
    //     if (colName != mmgHeader) {
    //       df3 = df3.drop(colName)
    //       // remaining csv header
    //       csvHeaderSet5 = csvHeaderSet5.filter(el => el != colName)
    //     } // .if
    

  } else {
    // there are multiple groups
    println("-oneSetOfCsvRepeats--multiple groups-->" + oneSetOfCsvRepeats )
    val numOfGroups = oneSetOfCsvRepeats.size / 3 // each is of (data, code, code_system)
    
    val listOfRepNums = List.range( 0, numOfGroups )
    
    println("listOfRepNums: --> " + listOfRepNums)
    
    var colsToCombine = oneSetOfCsvRepeats.filter( !_.contains(EXTENSION_CODE) )
    
    listOfRepNums.foreach( repNum => {
      
      val colName = oneSetOfCsvRepeats.filter( w => !w.contains(EXTENSION_CODE) && w.contains("[" + repNum + "]") )(0)
      //println("colName--> " + colName)
    
      val colNameCode = oneSetOfCsvRepeats.filter( w => ( w.contains("[" + repNum + "]") && w.contains(EXTENSION_CODE) && !w.contains(EXTENSION_CODE_SYSTEM) ) )(0) 
      //println("colNameCode--> " + colNameCode)
  
      val colNameCodeSystem = oneSetOfCsvRepeats.filter( w => ( w.contains(EXTENSION_CODE_SYSTEM) && w.contains("["+ repNum + "]") ) )(0)
      //println("colNameCodeSystem--> " + colNameCodeSystem)
      
      // combine data into colName( colName[x] will contain all 3 )
      
      // check if this group is not an repeat and not empty 
      if ( repNum > 0 ) {
        // check if these columns group are non nulls
        val colNameRowSet = df3.select("`" + colName + "`").map(r => r.getString(0)).collect.toSet.filter(_ != null)
        val colNameCodeRowSet = df3.select("`" + colNameCode + "`").map(r => r.getString(0)).collect.toSet.filter(_ != null)
        val colNameCodeSystemRowSet = df3.select("`" + colNameCodeSystem + "`").map(r => r.getString(0)).collect.toSet.filter(_ != null)
        
        println("colNameRowSet --> " + colNameRowSet)
        
        if ( colNameRowSet.size + colNameCodeRowSet.size + colNameCodeSystemRowSet.size == 0 ) {
          // drop it because this repeat is all empty
          df3.drop(colName, colNameCode, colNameCodeSystem)
          // take it ouf of columns to combine
          colsToCombine = colsToCombine.filter(_ != colName)
        } else {
          
         // df3 = df3.withColumn( colName, array(  col("`" + colNameCode + "`"), col("`" + colName + "`"), col("`" + colNameCodeSystem + "`") )  ) 
           df3 = df3.withColumn(colName, toDataCoded( col("`" + colNameCode + "`"), col("`" + colName + "`"), col("`" + colNameCodeSystem + "`")) )
                   .drop( colNameCode, colNameCodeSystem)
        } // .else
 
      } else {
       // df3 = df3.withColumn( colName, array(  col("`" + colNameCode + "`"), col("`" + colName + "`"), col("`" + colNameCodeSystem + "`") )  ) 
        df3 = df3.withColumn(colName, toDataCoded( col("`" + colNameCode + "`"), col("`" + colName + "`"), col("`" + colNameCodeSystem + "`")) )
                 .drop( colNameCode, colNameCodeSystem)
      } // .else
      
    }) // .listOfRepNums
    
    
    println("colsToCombine: --> " + colsToCombine)
    // combine colName[x] into array
    df3 = df3.withColumn( mmgHeader, array( colsToCombine.map(c=>col( "`" + c + "`" )):_* )  ) 
              .drop( oneSetOfCsvRepeats:_* )
    
    scrFilterOut += oneSetOfCsvRepeats.size 
    csvHeaderSet5 = csvHeaderSet5.filterNot(oneSetOfCsvRepeats.toSet)
    
  } // .else
  
}) // .singleCodedRepeatsSet

// display(df3.select("race_category", "binational_reporting_criteria"))
display(df3)

// COMMAND ----------

// check for step 4
println( csvHeaderSet4.size, csvHeaderSet5.size, scrFilterOut )
val checkSum4 = csvHeaderSet5.size == csvHeaderSet4.size - scrFilterOut
if ( !checkSum4 ) {
  throw new RuntimeException("code ref: checksum4")
} // .if

// println("diff 4 to 5: " + csvHeaderSet4.diff(csvHeaderSet5))

// COMMAND ----------

// 5. Elements ( $"block_type" =!= "Single" )  // Get all repeating Blocks 
//
// --------------------------------------------------  // && $"element_dataType" === "Coded" && $"element_isRepeat" === true
val blockNonSingle = mmgTable.filter( $"block_type" =!= BLOCK_TYPE_SINGLE )

val blockNonSingleArr = blockNonSingle.select("block_name_csv", "element_name_csv").map( r => (r.getString(0), r.getString(1))  ).collect 
 
val blockNonSingleMap = blockNonSingleArr.groupBy(_._1).map{ case(k,v) => k -> v.map(_._2).toList }


//println( "-blockNonSingleMap-> " + blockNonSingleMap.filterKeys(_ == "rvct_and_tbliss_epidemiology_laboratory_repeating_group_section") )

// COMMAND ----------

val bnsElementsAndTypeArr = blockNonSingle.select("element_name_csv", "element_dataType").map( r => (r.getString(0), r.getString(1)) ).collect 

// elements and type map
val bnsElementsAndTypeArrMap = bnsElementsAndTypeArr.toMap


// COMMAND ----------

val bnsBlocksArr = blockNonSingle.select("block_name_csv").map( r => (r.getString(0)) ).collect.toSet

// finding max repeats in the csv for each block
val bnsBlocksAndMaxRepsMap = bnsBlocksArr.map(blockName => {
  // get the actual blocks from the csv
  val blockRepeats = csvHeaderSet5.filter(hdr => hdr.contains(blockName))
  
  val blockRepNums = blockRepeats.map(blockName => {
    // extracting numbers between brackets with anything before or after
    val pattern =  """.*\[([\d]+)\].*""".r
    val pattern(num) = blockName
    num.toInt
  })
  
  (blockName -> blockRepNums.max)
}).toMap

println("bnsBlocksAndMaxRepsMap: " + bnsBlocksAndMaxRepsMap)

// COMMAND ----------

var df4 = df3


// import scala.collection.mutable.ListBuffer
// var allHeadersNonSingle = new ListBuffer[String]()

blockNonSingleMap.foreach{ case(blockName, elements) => {
  
  val csvColsForBlock = csvHeaderSet5.filter(h => h.contains(blockName)) // all this columns need to go in one, and these dropped

  // for each block repeat
  for ( rn <- List.range(0, bnsBlocksAndMaxRepsMap(blockName) + 1 ) ) { // repeat number 0 to max inclusive of max
    
    // find exact match of repeated block with number and element
    val csvBlockRepName = blockName + "[" + rn + "]"
    
    // combining the coded elements is arr of 3
    elements.foreach(el => {
      
       val csvColName = csvBlockRepName + "." + el 
      
        if ( bnsElementsAndTypeArrMap(el) == ELEMENT_DATATYPE_CODED ) {

          // full column (header) name && __code && __code_system
          val csvColNameCode = csvColName + EXTENSION_CODE 
          val csvColNameCodeSystem = csvColName + EXTENSION_CODE_SYSTEM 
          
          val csvColNameNoDot = csvBlockRepName + "_" + el
               
          // combine coded 
          // need to enclose in backticks because of dot (.) in column name
          //println("csvColNameNoDot: " + csvColNameNoDot + ", Coded, df4.columns.size(1): " + df4.columns.size + ", drop: " + csvColName + ", " + csvColNameCode + ", " + csvColNameCodeSystem)
          df4 = df4.withColumn( csvColNameNoDot, array( col("`" + csvColNameCode + "`"), col("`" + csvColName + "`"), col("`" + csvColNameCodeSystem + "`") )  ) 
              // drop does not work with backticks
              .drop( csvColName, csvColNameCode, csvColNameCodeSystem)
          //println("csvColNameNoDot: " + csvColNameNoDot + ", Coded, df4.columns.size(2): " + df4.columns.size)
        } else {
          // Not Coded, these elements need to go to Array to be able to be combined
          
          val csvColNameNoDot = csvBlockRepName + "_" + el
           
            //println("csvColNameNoDot: " + csvColNameNoDot + ", NC, df4.columns.size(1): " + df4.columns.size + ", drop: " + csvColName )
            df4 = df4.withColumn( csvColNameNoDot, array( col("`" + csvColName + "`") ) )
            .drop( csvColName )
           //println("csvColNameNoDot: " + csvColNameNoDot + ", NC, df4.columns.size(2): " + df4.columns.size)
        } // .if else
      
    }) // elements.foreach
    
    // getting the elements with current names 
    val dfElemCurrNames = elements.map( el => { csvBlockRepName + "_" + el }) 
         
    // combine all elements now for block repeat number (rn)
    println("blockName: " + blockName + ", csvBlockRepName: " + csvBlockRepName + ", dfElemCurrNames: " + dfElemCurrNames)
    
    
    // only non empty repeats after the first one
    if ( rn > 0 ) {
      // check to see if this repeat is not all empty, then would not be needed
      
      //println("rn: --> " + rn)
      
      val elemNonNull = dfElemCurrNames.map( cn => {
        
       // println("cn: --> " + cn)
        
        val elemSet = df4.select( cn ).map(r => r.getSeq(0)).collect.flatten.toSet

        if (elemSet.isInstanceOf[Set[Nothing]]) {
          0
        } else {
          1
        }
      }).reduce( _ + _ )
      
      if ( elemNonNull == 0) {
        // these elements from this rn are all null
        //println("elemNonNull == 0, drop dfElemCurrNames: --> " + dfElemCurrNames)
        df4 = df4.drop( dfElemCurrNames:_* )
        //println("df4.columns.size: ==> " + df4.columns.size)
      } else {
        
        // this repeat is still needed
       val dfElemCurrNamesTup = dfElemCurrNames.map( cn => { 
         val pattern =  """.*\]_(.*)""".r // get out the element name, everything after ]_
         val pattern(elemName) = cn
        ( elemName, cn ) // current csv (df) name, actual element name  
      }) // .dfElemCurrNames
      // println("blockName: " + blockName + ", csvBlockRepName: " + csvBlockRepName + ", dfElemCurrNamesTup: " + dfElemCurrNamesTup)

      dfElemCurrNamesTup.foreach{ case (elem, cn) => {
        // replace the column with (key, val)
        df4 = df4.withColumn( elem, lit(elem))
          .withColumn(cn, map(col(elem), col(cn)))
          .drop(elem)
      }} // .dfElemCurrNamesTup

      // println("blockName: " + blockName + ", combineMaps for: " + dfElemCurrNames)
      df4 = df4.withColumn( csvBlockRepName, map_concat( dfElemCurrNames.map(c => col(c)):_* ) )
        .drop( dfElemCurrNames:_* )
        
      } // .if ( elemNonNull == 0)
      
    } else { // .if ( rn > 0 )
      
      // this is the first repeat and keep it
       val dfElemCurrNamesTup = dfElemCurrNames.map( cn => { 
         val pattern =  """.*\]_(.*)""".r // get out the element name, everything after ]_
         val pattern(elemName) = cn
        ( elemName, cn ) // current csv (df) name, actual element name  
      }) // .dfElemCurrNames
      // println("blockName: " + blockName + ", csvBlockRepName: " + csvBlockRepName + ", dfElemCurrNamesTup: " + dfElemCurrNamesTup)

      dfElemCurrNamesTup.foreach{ case (elem, cn) => {
        // replace the column with (key, val)
        df4 = df4.withColumn( elem, lit(elem))
          .withColumn(cn, map(col(elem), col(cn)))
          .drop(elem)
      }} // .dfElemCurrNamesTup

      // println("blockName: " + blockName + ", combineMaps for: " + dfElemCurrNames)
      df4 = df4.withColumn( csvBlockRepName, map_concat( dfElemCurrNames.map(c => col(c)):_* ) )
        .drop( dfElemCurrNames:_* )
      
    }  // .if ( rn > 0 )
    
    
  } // .for each block repeat
  
  // combine all blocks repeats into 1 with array
  val blocksToCombine = df4.columns.filter(colName => colName.contains(blockName))
  
  //println("blockName: " + blockName + ", blocksToCombine: " + blocksToCombine.toSet + ", df4.columns.size(1): " + df4.columns.size)
  df4 = df4.withColumn(blockName, array( blocksToCombine.map(bl=>col(bl)):_*))
    .drop( blocksToCombine:_*)
  //println("df4.columns.size(2): " + df4.columns.size)
  
}} // .blockNonSingleMap.foreach


// display( df4 )

// COMMAND ----------

println("bnsBlocksArr: --> " + bnsBlocksArr)

// COMMAND ----------

// TODO: this part is not implemented at this time, to be coordinated with HL7 side

// change elements coded inside the maps from array of 3 --> DataCoded: code, value, codeSystem
// val codedArrToMap = udf ( ( arrMap: Array[Map[String, Array[String]]] ) => {
  
//   val am = arrMap.map(oneMap => {
    
//     val om = oneMap map { case (el,value) => {
      
//       if ( value.size == 3) {
//         //(el, DataCoded(Some(value(0)), Some(value(1)), Some(value(2))))
//         (el, value)
        
//       } else {
//         // TODO: - this is not coded! should change to just [value]
//         //(el, DataCoded(null, Some(value(0)), null ) )
//         (el, value)
//       } // .else

//     }} // .oneMap
    
//     om
  
//   }) // . arrMap.map
  
//   am

// }) // .codedArrToMap
  

// val df5 = bnsBlocksArr
//     .foldLeft(df4) { (dfcurr, cn) => { 
//       dfcurr.withColumn(cn, codedArrToMap(col(cn))) }}

// display(df5.select("repeating_variables_for_disease_exposure"))

val df5 = df4 


// COMMAND ----------

val addMetadata = udf ( ( pid: List[String], caseID: String, lastCase: Boolean, fileMessageID: String, fileModification: Timestamp, fileIngest: Timestamp ) => 

  new Metadata(
      /*ProfileIdentifier: String,*/ pid(0),
      /*CaseID: String,*/caseID, 
      /*LastCase: Boolean,*/lastCase,
      /*FileMessageID: String,*/fileMessageID,
      /*Source: String,*/SOURCE_CSV,
      /*FileModification*/fileModification,  // TODO: change to mod time
      /*FileIngest*/fileIngest,
    ) // .MetadataCDM
                       
) // .addMetadata


// COMMAND ----------

val mpisArr = df4.columns.filter(_.startsWith("message_profile_identifiers"))

val df6 = df5.withColumn("Metadata", addMetadata( $"message_profile_identifier", $"unique_case_id", lit(true), lit("FileMessageID"), $"__meta_ingestTimestamp", $"__meta_ingestTimestamp"/*TODO change to mod time*/ ) )
       .drop("unique_case_id", "source_format", "datetime_of_message")
       .drop( mpisArr:_* )
       .drop("__meta_ingestTimestamp")

display( df6 )

// COMMAND ----------

// sort dataframe 

val sortedDfCols = df6.columns.filter(!List( "Metadata").contains(_)).sortWith(_ < _)

val allDfCols = List("Metadata") ++ sortedDfCols.toList

val df7 = df6.select(allDfCols.map(el=>col(el)):_*)

// COMMAND ----------

// finally add to the CSV gold dataframe:


df7.write.format("delta")
    .option("mergeSchema", "true")
    .mode(SaveMode.Append) // TODO: change to overwrite
    .saveAsTable(XLRLake.getTableRef(XLRLake.CSVGoldTable))

// COMMAND ----------

println(df7.columns.size)
println(df7.columns.sortWith( _<_  ).toSeq)

// COMMAND ----------

display( df7 )


// COMMAND ----------


