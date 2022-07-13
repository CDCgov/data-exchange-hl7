// Databricks notebook source
// MAGIC %run ../common/adfParams

// COMMAND ----------

// MAGIC %run ../mmg/mmg_provider

// COMMAND ----------

// MAGIC %run ../vocab/vocab_loader_provider

// COMMAND ----------

val df1 = Lake.readDf(dbName, goldTable, isPipelineStreaming)
                .drop( "mmg_seq_singles", "mmg_seq_repeats" )
                .withColumn("content_validation_report_singles", lit(null)) // init column for validation report of singles


// COMMAND ----------

// map of profile -> mmg lines
val mmgMap = new MmgProvider(new MmgLoader).getAllMap

val profilesAvailable = mmgMap.keySet
//println($"profilesAvailable: -> $profilesAvailable")


// COMMAND ----------

// cumulate from the mmg df all the elements and all the blocks for foldLeft
// println( mmgMap )

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

// println("all mmg lines --> unique elementNames.length: --> " + elementNames.length)
// println("all mmg lines --> unique blockNames.length: --> " + blockNames.length)


// COMMAND ----------

// making a map for content validation: profileIdentifier -> element/block Name -> ( tuple of needed info for validation )

// mmgSeq
// 0 profileID, 1 blkType, 2 blkName, 3 blkOrdinal, 4  blkID, 
// 5 elemName,  6 elemOrdinal, 7 elemDataType, 8 elemIsRepeat, 9  elemValueSetCode, 
// 10 elemSetVersionNumber, 11 elemIdentifier, 12 elemSegmentType, 13 elemFieldPosition, 14 elemComponentPosition, 
// 15 elemCardinality, 16 hl7v2DataType, 17 - codeSystem, 18 blkNameStd, 19 elemNameStd 
val SEPARATOR = "~~"
// making a map for content validation: element name -> ( tuple of needed info for validation )
// ml(0) + SEPARATOR + ml(19)
val mmgElemSingleMap = singlesMmgLinesSeq.map( ml => ( ml(0) + SEPARATOR + ml(19), ( ml(1), ml(18), ml(19), ml(7), ml(8), ml(15), ml(9), ml(14), ml(12), ml(16), ml(17) ) ) ).toMap
//         1 - block_type, 2 - block_name_common, 3 - element_name_common, 
//         4 - element_dataType, 5 - element_isRepeat, 6 - element_cardinality, 
//         7 - element_valueSetCode, 8 - elementPositionForReport, 9 - elemSegmentType
//         10 - element_hl7v2DataType, 11 - element_codeSystem

// map element single to segment type
val mmgElemSingleSegTypeMap = singlesMmgLinesSeq.map( ml => (ml(19), ml(12)) ).toMap

// for repeats: ml(18) - block name
val mmgRepBlkMap = repeatsMmgLinesSeq.map( ml => (ml(0) + SEPARATOR + ml(18), "_") ).toMap // used to check if a repeated block belongs in a mmg

// ml(19) - elem name
val mmgElemRepeatsMap = repeatsMmgLinesSeq.map( ml => ( ml(0) + SEPARATOR + ml(19), ( ml(1), ml(18), ml(19), ml(7), ml(8), ml(15), ml(9), ml(14), ml(12), ml(16), ml(17) ) ) ).toMap
//         1 - block_type, 2 - block_name_common, 3 - element_name_common, 
//         4 - element_dataType, 5 - element_isRepeat, 6 - element_cardinality, 
//         7 - element_valueSetCode, 8 - elementPositionForReport, 9 - elemSegmentType
//         10 - element_hl7v2DataType, 11 - element_codeSystem

// COMMAND ----------

// one content validation report entry:

case class ValRepEntry(
  
  mmgBlkType: String, 
  mmgElemName: String, 
  mmgElemCard: String, 
  mmgElemType: String, 
  elType: String,
  elemValue: String, 
 // elemCoded: Option[ Map[String,String] ],
  message: String,
  
) // ./ ValRepEntry

// COMMAND ----------

// load vocabulary map

val vocabMap = new VocabProvider(new VocabLoader).getAllMap

//println("vocabMap.keySet: --> " + vocabMap.keySet)

// COMMAND ----------

val addVocabForValueSetCodes = udf ( ( codes: Seq[String] )  => {
  
  val t = codes.length match {
    case 0 => null
    
    case _ => {
      
      codes.map( code => {
        vocabMap.contains(code) match {
          case true => (code, vocabMap(code))
          case _ => null
        }
      }).toSeq
      
    } // case _
    
  } // codes.length match
  
  t.filter(_ != null).groupBy(_._1).map{ case(k,v) => k -> v.map(_._2).flatten.toSeq }
})// .addVocabForValueSetCodes

// COMMAND ----------

val df2 = df1.withColumn("vocab_entries_map", addVocabForValueSetCodes($"value_set_codes"))

//display( df2 )

// COMMAND ----------

// validating the elements singles:

val contentValidateElemSingle = udf ( ( elSegmentType: String, profileIdentifier: String, elName: String, elemDataType: Option[String], elemValue: Option[String], vocabEntriesMap: Map[String,Seq[(String, String, String)]], cvr: List[ValRepEntry] ) => {
  
  val elValue = elemValue.getOrElse("")
  val elDataType = elemDataType.getOrElse("")

  // Checking Cardinality - is required
  // ----------------------------------------

  val profIdentElName = profileIdentifier + SEPARATOR + elName 

  val validationRepEntries = mmgElemSingleMap.contains(profIdentElName) match {

      // this element is part of this mmg profile and needs validation
      case true => {
           // check is required
           // ---------------------------------------------
          val cardinality = mmgElemSingleMap(profIdentElName)._6
          val pattern =  """.*\[([\d]+)\..*""".r
          val pattern(num) = cardinality
          val cardFirstNum = num.toInt

//         1 - block_type, 2 - block_name_common, 3 - element_name_common, 
//         4 - element_dataType, 5 - element_isRepeat, 6 - element_cardinality, 
//         7 - element_valueSetCode, 8 - elementPositionForReport, 9 - elemSegmentType
//         10 - element_hl7v2DataType, 11 - element_codeSystem

          val vrCardinality = ( cardFirstNum, "".equals(elValue) ) match {
            
                  case (0, true) => null
            
                  case (1, true) => {
                    ValRepEntry(mmgElemSingleMap(profIdentElName)._1, 
                                         elName,
                                         cardinality,
                                         mmgElemSingleMap(profIdentElName)._10,
                                         elDataType, 
                                         elValue, 
                                         "element is required but not found") 
                  } // case (1, true) 
            
                  case (_, _) => null
          } // .val vrCardinality
        

         val vrDataType = ( elSegmentType, "".equals(elDataType), "".equals(elValue) ) match {

                case ("OBX", true, true) => null // value and dataType missing, cardinality will report it

                case ("OBX", true, false) => { // dataType missing and has value

                  ValRepEntry(mmgElemSingleMap(profIdentElName)._1, 
                                           elName,
                                           cardinality,
                                           mmgElemSingleMap(profIdentElName)._10,  // mmg element data type
                                           elDataType, 
                                           elValue,  
                                           "element data type not found") 
                }// .case ("OBX", true)

                case ("OBX", _, _) => { // check dataType from message agains MMG data type
                  val mmgDataType = mmgElemSingleMap(profIdentElName)._10

                    elDataType.equals(mmgDataType) match {
                      case true => null 
                      case _ => ValRepEntry(mmgElemSingleMap(profIdentElName)._1, 
                                                 elName,
                                                 cardinality,
                                                 mmgDataType, // mmg element data type
                                                 elDataType, 
                                                 elValue,  
                                                 "element data type not matching mmg data type") 
                    } // .match

                }// .case ("OBX", _, _)

                case (_, _, _) => null
           
          } // .val vrDataType
        
          val vrVocabCheck = ( elSegmentType, "".equals(elDataType), "".equals(elValue) ) match {

                case ("OBX", true, true) => null // value and dataType missing, cardinality will report it

                case ("OBX", true, false) =>  null // dataType missing and has value, vrDataType will report it
 
                case ("OBX", _, _) => {  // has dataType and value
                  
                  val mmgDataType = mmgElemSingleMap(profIdentElName)._10

                  elDataType.equals(mmgDataType) && elDataType.equals("CWE") match {
                    
                      case true => {
                        
                        elValue.split("~").map( elValue => {
                          
                            // this is a CWE -> check against vocabulary
                            val elParts = elValue.split("\\^")
                            val (cweElConceptCode, cweElValue, cweElCodeSys) = elParts.length match { // N^No^HL70136
                                                                                      case x if x >= 3 => (elParts(0), elParts(1), elParts(2))
                                                                                      case 2 => (elParts(0), elParts(1), "")
                                                                                      case _ => ("", "", "")
                            } // val (elConceptCode, elValue, elCodeSys) ..match
                            val mmgElValueSetCode = mmgElemSingleMap(profIdentElName)._7

                            vocabEntriesMap.contains(mmgElValueSetCode) match {

                              case true => {
                                // check cweElConceptCode, cweElValue 
                                val vocabEntries = vocabEntriesMap(mmgElValueSetCode) // Seq[(String, String, String)] // "hl7_table_0396_code", "code_system_oid", "concept_code"
                                val vocabEntryMatch = vocabEntries.filter{ case (codeSysCode, codeSysOid, conceptCode) =>{
                                  ( ( cweElCodeSys == codeSysCode || cweElCodeSys == codeSysOid ) && cweElConceptCode == conceptCode ) 
                                }}
                                vocabEntryMatch.length match {
                                  case 0 => ValRepEntry(mmgElemSingleMap(profIdentElName)._1, // vocab entry not found for message values
                                                     elName,
                                                     cardinality,
                                                     mmgDataType, // mmg element data type
                                                     elDataType, 
                                                     elValue,  
                                                     "vocabulary code system code and code concept not found in vocabulary entries for: " + mmgElValueSetCode) 
                                  case _ => null // message values match vocab ok
    //                               ValRepEntry(mmgElemSingleMap(profIdentElName)._1, // vocab entry available
    //                                                  elName,
    //                                                  cardinality,
    //                                                  mmgDataType, // mmg element data type
    //                                                  elDataType, 
    //                                                  elValue,  
    //                                                  "FOUND: -> " + mmgElValueSetCode + " -> " + vocabEntryMatch(0)._1 + ", " + vocabEntryMatch(0)._2 + ", " + vocabEntryMatch(0)._3 ) 
                                }
                              } // case true

                              case _ => ValRepEntry(mmgElemSingleMap(profIdentElName)._1, // vocab entry not available
                                                     elName,
                                                     cardinality,
                                                     mmgDataType, // mmg element data type
                                                     elDataType, 
                                                     elValue,  
                                                     "vocabulary not available for code system code: " + mmgElValueSetCode) 
                            } // match
                          
                        }) // . elValue.split("~").map
                        
                      }  // .case true
                    
                      case _ => null// vrDataType will report it only if there is data type mismatch between message and mmg, otherwise this is not a CWE
                    
                  } // .match

                }// .case ("OBX", false, _)

                case (_, _, _) => null
           
          } // .val vrVocabCheck
          
          
          // return the issues if there are any or null
        vrVocabCheck match {
          
          case null => List(vrCardinality, vrDataType).filter( _ != null ) 
          case _ => List(vrCardinality, vrDataType).filter( _ != null ) ++ vrVocabCheck.filter( _ != null ).toList
          
        } // vrVocabCheck
         
      } // .case true

      case _ => null // this element name is not in this profile identifier, no validation is needed

  } // .val vrEntryCardinality 
  
  validationRepEntries match {
    case null => cvr
    case _ => {
        // check if there is a report and/or report entries to add
       ( cvr, validationRepEntries.length ) match {
          case ( null, 0 ) => null
          case ( null, _ ) => validationRepEntries
          case ( _, 0 ) => cvr
          case ( _, _ ) => validationRepEntries ++ cvr
        } // ./ match
    } // case _
  } // .validationRepEntries


}) // .contentValidateElemSingle

// COMMAND ----------

val df3 = elementNames.foldLeft(df2) { (acc, elName) => {
  
//         1 - block_type, 2 - block_name_common, 3 - element_name_common, 
//         4 - element_dataType, 5 - element_isRepeat, 6 - element_cardinality, 
//         7 - element_valueSetCode, 8 - elementPositionForReport, 9 - elemSegmentType
//         10 - element_hl7v2DataType, 11 - element_codeSystem
  
  mmgElemSingleSegTypeMap(elName) match {
    
    case "OBX" => 
      acc.withColumn("content_validation_report_singles", contentValidateElemSingle( lit("OBX"), $"__message_info.profile_identifier", lit(elName), col(elName + "._1"), col(elName + "._2"), $"vocab_entries_map", $"content_validation_report_singles" ) ) 
    case _ =>  
      acc.withColumn("content_validation_report_singles", contentValidateElemSingle( lit("_"), $"__message_info.profile_identifier", lit(elName), lit(null), col(elName), $"vocab_entries_map", $"content_validation_report_singles" ) ) 
  
  } // .match 
  
}} // .df3


//display( df3.select("content_validation_report_singles") )


// COMMAND ----------


val contentValidateRepBlks = udf( (profileIdentifier: String, blkName: String, repeat: Array[Map[String, Tuple2[String, String] ]], vocabEntriesMap: Map[String,Seq[(String, String, String)]], cvr: List[ValRepEntry] ) => {
  // repeat: [{"country_of_exposure": {"_1": "CWE", "_2": "DOM^DOMINICAN REPUBLIC^ISO3166_1"}, "city_of_exposure": {"_1": "ST", "_2": "Punta Cana"}}]
  
  val profIdentBlkName = profileIdentifier + SEPARATOR + blkName 
  
  val validationRepEntries = ( mmgRepBlkMap.contains(profIdentBlkName) ) match {
    case true => {// need to check this repeat
        repeat match {
          case null => null// TODO: for null repeat check if any required elements and issue vr issue if missing
          case _ => {
              repeat.map(repeatsMap => {
                  repeatsMap.map{ case(elName, elEntries) => {
                      // check this element elName 
                      val profIdentElName = profileIdentifier + SEPARATOR + elName 
                      val ml = mmgElemRepeatsMap(profIdentElName)
                      //         1 - block_type, 2 - block_name_common, 3 - element_name_common, 
                      //         4 - element_dataType, 5 - element_isRepeat, 6 - element_cardinality, 
                      //         7 - element_valueSetCode, 8 - elementPositionForReport, 9 - elemSegmentType
                      //         10 - element_hl7v2DataType, 11 - element_codeSystem
                      val elValue = elEntries._2
                      val elDataType = elEntries._1

                      // check is required
                      // ---------------------------------------------
                      val cardinality = ml._6
                      val pattern =  """.*\[([\d]+)\..*""".r
                      val pattern(num) = cardinality
                      val cardFirstNum = num.toInt


                      val vrCardinality = ( cardFirstNum, "".equals(elValue) ) match {

                              case (0, true) => null

                              case (1, true) => { ValRepEntry(ml._1, 
                                                             elName,
                                                             cardinality,
                                                             ml._4, // mmg element data type
                                                             elDataType, 
                                                             elValue, 
                                                             "element is required but not found") 
                              } // case (1, true) 

                              case (_, _) => null
                      } // .val vrCardinality

                     val vrDataType = ( "".equals(elDataType), "".equals(elValue) ) match {

                          case (true, true) => null // value and dataType missing, cardinality will report it

                          case (true, false) => { // dataType missing and has value

                            ValRepEntry(ml._1, 
                                       elName,
                                       cardinality,
                                       mmgElemRepeatsMap(profIdentElName)._10,  // mmg element data type
                                       elDataType, 
                                       elValue,  
                                       "element data type not found") 
                          }// .case ("OBX", true)

                          case ( _, _) => { // check dataType from message agains MMG data type
                            val mmgDataType = mmgElemRepeatsMap(profIdentElName)._10

                              elDataType.equals(mmgDataType) match {
                                case true => null 
                                case _ => ValRepEntry(ml._1, 
                                                     elName,
                                                     cardinality,
                                                     mmgDataType, // mmg element data type
                                                     elDataType, 
                                                     elValue,  
                                                     "element data type not matching mmg data type") 
                              } // .match

                          } // case (_, _)

                      } // .val vrDataType
                      
                      val vrVocabCheck = ( "".equals(elDataType), "".equals(elValue) ) match {

                        case (true, true) => null // value and dataType missing, cardinality will report it

                        case (true, false) =>  null // dataType missing and has value, vrDataType will report it

                        case  (_, _) => {  // has dataType and value

                          val mmgDataType = mmgElemRepeatsMap(profIdentElName)._10

                          elDataType.equals(mmgDataType) && elDataType.equals("CWE") match {

                              case true => {

                                elValue.split("~").map( elValue => {

                                    // this is a CWE -> check against vocabulary
                                    val elParts = elValue.split("\\^")
                                    val (cweElConceptCode, cweElValue, cweElCodeSys) = elParts.length match { // N^No^HL70136
                                                                                              case x if x >= 3 => (elParts(0), elParts(1), elParts(2))
                                                                                              case 2 => (elParts(0), elParts(1), "")
                                                                                              case _ => ("", "", "")
                                    } // val (elConceptCode, elValue, elCodeSys) ..match
                                    val mmgElValueSetCode = mmgElemRepeatsMap(profIdentElName)._7

                                    vocabEntriesMap.contains(mmgElValueSetCode) match {

                                      case true => {
                                        // check cweElConceptCode, cweElValue 
                                        val vocabEntries = vocabEntriesMap(mmgElValueSetCode) // Seq[(String, String, String)] // "hl7_table_0396_code", "code_system_oid", "concept_code"
                                        val vocabEntryMatch = vocabEntries.filter{ case (codeSysCode, codeSysOid, conceptCode) =>{
                                          ( ( cweElCodeSys == codeSysCode || cweElCodeSys == codeSysOid ) && cweElConceptCode == conceptCode ) 
                                        }}
                                        vocabEntryMatch.length match {
                                          case 0 => ValRepEntry(ml._1, // vocab entry not found for message values
                                                             elName,
                                                             cardinality,
                                                             mmgDataType, // mmg element data type
                                                             elDataType, 
                                                             elValue,  
                                                             "vocabulary code system code and code concept not found in vocabulary entries for: " + mmgElValueSetCode) 
                                          case _ => null // message values match vocab ok
            //                               ValRepEntry(mmgElemSingleMap(profIdentElName)._1, // vocab entry available
            //                                                  elName,
            //                                                  cardinality,
            //                                                  mmgDataType, // mmg element data type
            //                                                  elDataType, 
            //                                                  elValue,  
            //                                                  "FOUND: -> " + mmgElValueSetCode + " -> " + vocabEntryMatch(0)._1 + ", " + vocabEntryMatch(0)._2 + ", " + vocabEntryMatch(0)._3 ) 
                                        }
                                      } // case true

                                      case _ => ValRepEntry(ml._1, // vocab entry not available
                                                             elName,
                                                             cardinality,
                                                             mmgDataType, // mmg element data type
                                                             elDataType, 
                                                             elValue,  
                                                             "vocabulary not available for code system code: " + mmgElValueSetCode) 
                                    } // match

                                }) // . elValue.split("~").map

                              }  // .case true

                              case _ => null// vrDataType will report it only if there is data type mismatch between message and mmg, otherwise this is not a CWE

                          } // .match

                        } // case (_, _)

                  } // .val vrVocabCheck
                    
                  vrVocabCheck match {
          
                    case null => List(vrCardinality, vrDataType).filter( _ != null ) 
                    case _ => List(vrCardinality, vrDataType).filter( _ != null ) ++ vrVocabCheck.filter( _ != null ).toList

                  } // vrVocabCheck

                  }}.flatten // .repeatsMap
               }).flatten.toList

          } // .case _
        } // .repeat match
    
      //cvr
    } // .case true, //. check this repeat
    
    case _ => null // this repeat is not from this mmg or is null
      
  }// .match
  
    validationRepEntries match {
      case null => cvr
      case _ => {
          val validationRepEntriesReals = validationRepEntries.filter( _ != null)
          // check if there is a report and/or report entries to add
         ( cvr, validationRepEntriesReals.length ) match {
            case (null, 0) => null 
            case ( null, _ ) => validationRepEntriesReals
            case ( _, 0 ) => cvr
            case ( _, _ ) => validationRepEntriesReals ++ cvr
          } // ./ match
      } // case _
    } // .validationRepEntries
  
})

// COMMAND ----------

// init report for repeats
val df4 = df3.withColumn("content_validation_report_repeats", lit(null))

// COMMAND ----------

// check repeats blockNames
val df5 = blockNames.foldLeft(df4) { (acc, blkName) => {
  
//         1 - block_type, 2 - block_name_common, 3 - element_name_common, 
//         4 - element_dataType, 5 - element_isRepeat, 6 - element_cardinality, 
//         7 - element_valueSetCode, 8 - elementPositionForReport, 9 - elemSegmentType
//         10 - element_hl7v2DataType, 11 - element_codeSystem
// println(blkName)
  
  acc.withColumn("content_validation_report_repeats", contentValidateRepBlks( $"__message_info.profile_identifier", lit(blkName), col(blkName), $"vocab_entries_map", $"content_validation_report_repeats" ) )
  
}} // .df5

//display( df5.select("content_validation_report_singles", "content_validation_report_repeats"))

// COMMAND ----------

// merge reports for singles and repeats
val mergeCVReports = udf( (cvr1: List[ValRepEntry], cvr2: List[ValRepEntry]) => {
 cvr1 ++ cvr2
}) // .mergeReports


val df6 = df5.withColumn("content_validation_report", 
                             when( $"content_validation_report_singles".isNotNull and $"content_validation_report_repeats".isNotNull, mergeCVReports($"content_validation_report_singles", $"content_validation_report_repeats") )
                               .otherwise(
                                   when( $"content_validation_report_singles".isNotNull, $"content_validation_report_singles" )
                                         .otherwise( 
                                             when( $"content_validation_report_repeats".isNotNull, $"content_validation_report_repeats")
                                                 .otherwise( null ) 
                                         ) // .otherwise
                               ) // .otherwise
                        ) // .withColumn
              .drop("content_validation_report_singles", "content_validation_report_repeats")
                         
//display( df6 )

// COMMAND ----------

// clean up data in obx, remove the data type and leave only value

// mmgSeq
// 0 profileID, 1 blkType, 2 blkName, 3 blkOrdinal, 4  blkID, 
// 5 elemName,  6 elemOrdinal, 7 elemDataType, 8 elemIsRepeat, 9  elemValueSetCode, 
// 10 elemSetVersionNumber, 11 elemIdentifier, 12 elemSegmentType, 13 elemFieldPosition, 14 elemComponentPosition, 
// 15 elemCardinality, 16 hl7v2DataType, 17 - codeSystem, 18 blkNameStd, 19 elemNameStd 

val elementObxNames = singlesMmgLinesSeq.filter( _(12) == "OBX" ).map( _(19) ).toSet.toSeq // only unique names

val getElemObxValue = udf((element: Option[Tuple2[String, String]]) => {
  
  element.orNull match {
    case null => null
    case _ => element.get._2
  } // .element.orNull
  
})

val df7 = elementObxNames.foldLeft(df6) { (acc, elName) => {
  
  acc.withColumn(elName, getElemObxValue(col(elName)))
 
}} // .df7

// display( df7 )

// COMMAND ----------

// sort columns 
val metaCols = List("__metadata", "__message_info", "mmwr_year_partition", "mmg_seq", "value_set_codes", "vocab_entries_map", "content_validation_report")

val sortedCols = df7.columns.filter(!metaCols.contains(_)).sortWith(_ < _)

val allCols = metaCols ++ sortedCols.toList

val df8 = df7.select(allCols.map(el=>col(el)):_*)

// display( df8 )

// COMMAND ----------

Lake.writeDf(df8, dbName, goldWithContValRep, mntDelta, "mmwr_year_partition", isPipelineStreaming)

