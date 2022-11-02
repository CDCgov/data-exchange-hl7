package gov.cdc.dataexchange.entModel

object TransformerGoldTemp {

    // returns: ( obxsEpiNotInMmg, cdm )
    def obxsEpiToCDM( obxsEpi: Seq[(String, String, String, String)], mmgSeq: Seq[Seq[String]] ): 
        Option[Map[String, Equals]] = {
        
        // TODO: guard on existing profiles
    
        // mmgSeq
        // 0 profileID, 1 blkType, 2 blkName, 3 blkOrdinal, 4  blkID, 
        // 5 elemName,  6 elemOrdinal, 7 elemDataType, 8 elemIsRepeat, 9  elemValueSetCode, 
        // 10 elemSetVersionNumber, 11 elemIdentifier, 12 elemSegmentType, 13 elemFieldPosition, 14 elemComponentPosition, 
        // 15 elemCardinality, 16 hl7v2DataType, 17 - codeSystem, 18 blkNameStd, 19 elemNameStd 

        if ( mmgSeq == null) { return None }
        // making quick access maps from mmg rows
        val mmgObxIdentifierMap = mmgSeq.map( ml => {
        ( ml(11), ml )  // ml(11) elemIdentifier, ml one mmg line
        }).toMap

        
        // return at end obxsEpiNotInMmg
        val ( obxsEpiInMmg, obxsEpiNotInMmg ) = obxsEpi.partition( ob => { // split into 2 lists wheter or not the message obx identifier is in the mmg
        
            val obxLine = ob._4 // the 4th tuple element is the obx
            val obxParts = obxLine.split("\\|")
            val obxIdentifier = obxParts(3).split("\\^")(0)
        
            // check if this obxIdentifier is in the Mmg
            mmgObxIdentifierMap.contains(obxIdentifier) match { 
            case true => true
            case _ => false
            } // .match
        
        }) // .val
    
        obxsEpiInMmg.length match {
        case 0 => None //( obxsEpiInMmg, obxsEpiNotInMmg, null, null, null, null, null )
        
        // there is obx info to convert to cdm:
        case _ => {
            
            
            // obxsEpiInMmg Tuple of (MSH, PID, OBR, OBX)
            

            // MSH
            val mshLine = obxsEpiInMmg.head._1 // only one MSH segment
            val mmgMshs = mmgSeq.filter( ml => ml(12) == "MSH") // ml - one mmg line, ml(12) elemSegmentType

            // for each needed MSH segment present in Mmg
            val mshDataElemMap = mmgMshs.map( ml => {

                // get msh info
                val elemFieldPosition = ml(13).toInt - 1 // ml(13) elemFieldPosition
                //val elemIsRepeat = Try( ml(8).toBoolean).getOrElse(false)

                val mshLineParts = mshLine.split("\\|")

                val mshData = mshLineParts.size match {
                    case x if x > elemFieldPosition => mshLineParts(elemFieldPosition) 
                    case _ => null
                } // 

                // mmg element name
                val elemNameStd = ml(19) // 19 elemNameStd 

                ( elemNameStd, ( "MSH", mshData ) ) // e.g. (message_profile_identifier,(MSH,NOTF_ORU_v3.0^PHINProfileID^2.16.840.1.114222.4.10.3^ISO~Generic_MMG_V2.0^PHINMsgMapID^2.16.840.1.114222.4.10.4^ISO))
            }).toMap // .mshDataElemMap
            //mshDataElemMap.foreach( el => println( " --> " + el ))
            // .MSH


            // PID
            val pidLine = obxsEpiInMmg.head._2 // only one PID segment
            val mmgPids = mmgSeq.filter( ml => ml(12) == "PID") // ml(12) elemSegmentType

            // for each needed PID segment present in Mmg
            val pidDataElemMap = mmgPids.map( ml => {

                // get msh info
                val elemFieldPosition = ml(13).toInt  // ml(13) elemFieldPosition
                val elemComponentPosition = ml(14).toInt - 1  // ml(14) elemComponentPosition
                //val elemIsRepeat = Try( ml(8).toBoolean).getOrElse(false)

                val pidLineParts = pidLine.split("\\|")

                val pidData = pidLineParts.size match {

                    case x if x > elemFieldPosition => {

                    val elem = pidLineParts(elemFieldPosition)

                    elemComponentPosition match {
            //               case x if x > 0 => {
            //                 val comp = elem.split("\\^")
            //                 comp.size match {
            //                     case y if y > elemComponentPosition => comp(elemComponentPosition)
            //                     case _ => comp
            //                 }
            //               }
                        case _ => elem
                    } // .match

                    } // .case

                    case _ => null
                } // .match


                // mmg element name
                val elemNameStd = ml(19)  // 19 elemNameStd 

                ( elemNameStd, ( "PID", pidData ) ) //  e.g. (subject_address_zip_code,(PID,77018))
            }).toMap // .pidDataElemMap
            //pidDataElemMap.foreach( el => println( " --> " + el ))
            // .PID


            // OBR
            val obrLine = obxsEpiInMmg.head._3 // tuple 3rd elem is the OBR, there is only one the Epi OBR in obxsEpi ( obxsEpiInMmg )
            val mmgObrs = mmgSeq.filter( ml => ml(12) == "OBR") // ml(12) elemSegmentType

            // for each needed OBR segment present in Mmg
            val obrDataElemMap = mmgObrs.map( ml => {

                // get msh info
                val elemFieldPosition = ml(13).toInt // ml(13) elemFieldPosition
                val elemComponentPosition = ml(14).toInt - 1 // ml(14) elemComponentPosition
                // val elemIsRepeat = Try( ml(8).toBoolean).getOrElse(false) 

                val obrLineParts = obrLine.split("\\|")

                val obrData = obrLineParts.size match {

                    case x if x > elemFieldPosition => {

                    val elem = obrLineParts(elemFieldPosition)

                    elemComponentPosition match {
        //                 case x if x > 0 => {
        //                   val comp = elem.split("\\^")
        //                   comp.size match {
        //                       case y if y > elemComponentPosition => comp(elemComponentPosition)
        //                       case _ => comp
        //                   }
        //                 }
                        case _ => elem
                    } // .match

                    } // .case

                    case _ => null
                } // .match


                // mmg element name
                val elemNameStd = ml(19)

                ( elemNameStd, ( "OBR", obrData ) ) 
            }).toMap // .obrDataElemMap
            //obrDataElemMap.foreach( el => println( " --> " + el ))
            // .OBR


            // OBX

            // SINGLE OBX's  ( blkType is Single )
            val singleObxsIdentifier = mmgSeq.filter( _(1) == "Single").map(_(11)) // elemIdentifier

            // separate the elements by Singles and RepBlks elements
            val ( obxsEpiInMmgSingles, obxsEpiInMmgElemRepBlks ) = obxsEpiInMmg.partition( line => {
                val obxLine = line._4  // tuple 4th elem is the OBX
                val obxParts = obxLine.split("\\|")
                val elemIdentifier = obxParts(3).split("\\^")(0)
                singleObxsIdentifier.contains(elemIdentifier)
            })
            // println( obxsEpiInMmgSingles.size, obxsEpiInMmgRepBlks.size )

            val obxDataElemMapSingles = obxsEpiInMmgSingles.map( line => {

                val obxLine = line._4 // tuple 4th elem is the OBX

                val obxParts = obxLine.split("\\|")
                
                // get obx info
                val (obxDataType, elemIdentifier, obxData, elemNameStd) = obxParts.length match {
                case x if x > 5 => {
                    val obxDataType = obxParts(2)
                    val elemIdentifier = obxParts(3).split("\\^")(0)
                    val obxData = obxParts(5)
                    
                    // mmg element name
                    val elemNameStd = mmgObxIdentifierMap(elemIdentifier)(19) // elemNameStd
                    (obxDataType, elemIdentifier, obxData, elemNameStd) // leave as is, in the notebook only (obxDataType, obxData) is selected for singles
                } // .case x if x > 5
                
                case x if x > 4 && x <= 5 => {
                    val obxDataType = obxParts(2)
                    val elemIdentifier = obxParts(3).split("\\^")(0)
                    val obxData = ""
                    
                    // mmg element name
                    val elemNameStd = mmgObxIdentifierMap(elemIdentifier)(19) // elemNameStd
                    (obxDataType, elemIdentifier, obxData, elemNameStd)
    
                    } // .case x if x > 4 && x <= 5
                
                case _ => {
                    ("", "", "", "error_missing_obx_element_identifier") 
                }
                
                }// ./get obx info


                ( elemNameStd, ( "OBX", obxDataType, elemIdentifier, obxData ) ) // e.g. (reporting_state,(OBX,CWE,77966-0,48^Texas^FIPS5_2))
            }).toMap
            // obxDataElemMapSingles.foreach( el => println(" --> " + el ))

            val SEPARATOR = "~~"

            // group by mmgblkID + blockNum, each block num
            val obxsEpiInMmgElemRepBlksMap1 = obxsEpiInMmgElemRepBlks.map( line => {
                val obxLine = line._4 // tuple 4th elem is the OBX
                val obxParts = obxLine.split("\\|")
                
                // get obx info
                val (obxDataType, elemIdentifier, obxBlockNum, obxData, blkNameStd) = obxParts.length match {
                
                case x if x > 5 => { 
                    val obxDataType = obxParts(2)
                    val elemIdentifier = obxParts(3).split("\\^")(0)
                    val obxBlockNum = obxParts(4)
                    val obxData = obxParts(5)
                    
                    // mmg block name
                    val blkNameStd = mmgObxIdentifierMap(elemIdentifier)(18) // blkNameStd
                    
                    (obxDataType, elemIdentifier, obxBlockNum, obxData, blkNameStd)
                } // case x if x >= 5
                
                case x if x > 4 && x <= 5 => {
                    val obxDataType = obxParts(2)
                    val elemIdentifier = obxParts(3).split("\\^")(0)
                    val obxBlockNum = obxParts(4)
                    val obxData = ""
                    
                    // mmg block name
                    val blkNameStd = mmgObxIdentifierMap(elemIdentifier)(18) // blkNameStd
                    
                    (obxDataType, elemIdentifier, obxBlockNum, obxData, blkNameStd)
                    } // .case x if x > 4 && x <= 5
                
                    case _ => {
                    //  (obxDataType, elemIdentifier, obxBlockNum, obxData, blkNameStd)
                    ("", "", "error_missing_obx_element_identifier", "", "error_missing_obx_element_identifier")
                    } // case _
                }// ./get obx info
            

                ( blkNameStd + SEPARATOR + obxBlockNum, ("OBX", obxDataType, elemIdentifier, obxData) )
                
            }).groupBy(_._1).map{ case (obxBlockID, lines) => obxBlockID -> lines.map(_._2).toSeq } 
            // (eac0fcf3-a9e3-4e4a-a731-ec223c4b2ceb~~2,ArrayBuffer((OBX,CWE,77984-3,USA^UNITED STATES OF AMERICAxx^ISO3166_1), (OBX,CWE,77985-0,48^Texasxx^FIPS5_2), (OBX,ST,77986-8,Houstonxx), (OBX,ST,77987-6,Harrisxx)))

            // group only by mmgblkID, full block
            val obxsEpiInMmgElemRepBlksMap2 = obxsEpiInMmgElemRepBlksMap1.map { 

                case (blkNameStdBlkNum, elementLinesMap) => {

                    Map( blkNameStdBlkNum.split(SEPARATOR)(0) -> elementLinesMap )

                } // .case

            }.flatten.groupBy(_._1).map{ case (obxBlockID, elemList) => obxBlockID -> elemList.map(_._2).toSeq } 
            // (repeating_variables_for_disease_exposure,List(ArrayBuffer((OBX,CWE,77984-3,USA^UNITED STATES OF AMERICAxx^ISO3166_1), (OBX,CWE,77985-0,48^Texasxx^FIPS5_2), (OBX,ST,77986-8,Houstonxx), (OBX,ST,77987-6,Harrisxx)), 
            // ArrayBuffer((OBX,CWE,77984-3,USA^UNITED STATES OF AMERICA^ISO3166_1), (OBX,CWE,77985-0,48^Texas^FIPS5_2), (OBX,ST,77986-8,Houston), (OBX,ST,77987-6,Harris)))

            val obxDataElemRepBlks = obxsEpiInMmgElemRepBlksMap2.map { 

                case (blkNameStd, elementListCombined) => {

                    val elemRepOneMap = elementListCombined.map( elementLines => {

                        elementLines.map( obxInfo => {

                        val elemNameStd = mmgObxIdentifierMap(obxInfo._3)(19) // elemNameStd

                    // Map( elemNameStd -> obxInfo )
                        Map( elemNameStd -> (obxInfo._2, obxInfo._4) ) 

                        }).reduce( _ ++ _ )

                    }) // .elemRepOneMap

                    ( blkNameStd, elemRepOneMap)
                } // .case                   
            }//.obxDataElemRepBlks
            // Map(repeating_variables_for_disease_exposure -> List(Map(country_of_exposure -> (OBX,CWE,77984-3,USA^UNITED STATES OF AMERICAxx^ISO3166_1), state_or_province_of_exposure -> 
            // (OBX,CWE,77985-0,48^Texasxx^FIPS5_2), city_of_exposure -> (OBX,ST,77986-8,Houstonxx), county_of_exposure -> (OBX,ST,77987-6,Harrisxx)), 
            // Map(country_of_exposure -> (OBX,CWE,77984-3,USA^UNITED STATES OF AMERICA^ISO3166_1), state_or_province_of_exposure -> (OBX,CWE,77985-0,48^Texas^FIPS5_2), 
            // city_of_exposure -> (OBX,ST,77986-8,Houston), county_of_exposure -> (OBX,ST,77987-6,Harris))))

            // obxDataElemRepBlks.foreach( el => println( " --> " + el ))
            // .OBX


            val cdm = mshDataElemMap ++ pidDataElemMap ++ obrDataElemMap ++ obxDataElemMapSingles ++ obxDataElemRepBlks 

            // TODO: add element component position to pidDataElemMap and obrDataElemMap -> change type -> UnsupportedOperationException: Schema for type java.io.Serializable is not supported

            // ( obxsEpiInMmg, obxsEpiNotInMmg, mshDataElemMap, pidDataElemMap, obrDataElemMap, obxDataElemMapSingles, obxDataElemRepBlks )
            Option(cdm)
        } // .case _ 
        } // .obxsEpiInMmg.length

    
    } // .obxsEpiToCDM

} // TransformerGoldTemp 






