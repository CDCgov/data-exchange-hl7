package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.redisModels.Block
import gov.cdc.dex.redisModels.Element
import gov.cdc.dex.redisModels.ValueSetConcept

import gov.cdc.dex.hl7.model.PhinDataType

import gov.cdc.dex.util.StringUtils

import com.google.gson.Gson 
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

// import redis.clients.jedis.DefaultJedisClientConfig
// import redis.clients.jedis.Jedis

import  gov.cdc.dex.azure.RedisProxy 

class Transformer(val redisProxy: RedisProxy)  {

    val redisClient = redisProxy.getJedisClient()

    companion object {
        val logger = LoggerFactory.getLogger(Transformer::class.java.simpleName)
        private val gson = Gson()
        private val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()

        const val MMG_BLOCK_TYPE_SINGLE = "Single"
        private val OBR_4_1_EPI_ID = "68991-9"
        private val MMG_BLOCK_NAME_MESSAGE_HEADER = "Message Header" 
        // private val MMG_BLOCK_NAME_SUBJECT_RELATED = "Subject Related"
        private val REDIS_VOCAB_NAMESPACE = "vocab:"
        private val ELEMENT_CE = "CE"
        private val ELEMENT_CWE = "CWE"
        // private val PHIN_DATA_TYPE_KEY_NAME = "phin_data_type" // only used in dev
        private val CODE_SYSTEM_CONCEPT_NAME_KEY_NAME = "code_system_concept_name"
        private val CDC_PREFERRED_DESIGNATION_KEY_NAME =  "cdc_preferred_designation"
    }

        // --------------------------------------------------------------------------------------------------------
        //  ------------- hl7ToJsonModelBlocksSingle ------------- BLOCKS SINGLE
        // --------------------------------------------------------------------------------------------------------
        
        //? @Throws(Exception::class) 
        fun hl7ToJsonModelBlocksSingle(hl7Content: String, mmgsArr: Array<MMG>): Map<String, Any?> {

            // val redisClient = redisProxy.getJedisClient()

            // there could be multiple MMGs each with MSH, PID -> filter out and only keep the one's from the last MMG 
            val mmgs = getMmgsFiltered(mmgsArr)

            val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks

            val (mmgBlocksSingle, _) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }

            val messageLines = getMessageLines(hl7Content)

            val mmgElemsBlocksSingle = mmgBlocksSingle.flatMap { it.elements } // .mmgElemsBlocksSingle
            // logger.info("mmgElemsBlocksSingle.size: --> ${mmgElemsBlocksSingle.size}")

            //  ------------- MSH
            // ----------------------------------------------------
            val mmgMsh = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "MSH" }

            val mshMap = mmgMsh.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition - 1

                val mshLineParts = messageLines.filter { it.startsWith("MSH|") }[0].split("|") // there would be only one MSH

                val segmentDataFull = if (mshLineParts.size > dataFieldPosition) mshLineParts[dataFieldPosition].trim() else null 

                val segmentData = if ( segmentDataFull.isNullOrEmpty() ) null else getSegmentData(el, segmentDataFull)
                
                StringUtils.normalizeString(el.name) to segmentData
            }.toMap() // .mmgMsh.map


            //  ------------- PID
            // ----------------------------------------------------
            val mmgPid = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "PID" }

            val pidMap = mmgPid.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition

                val pidLineParts = messageLines.filter { it.startsWith("PID|") }[0].split("|") // there would be only one PID

                val segmentDataFull = if (pidLineParts.size > dataFieldPosition) pidLineParts[dataFieldPosition].trim() else null 

                val segmentData = if ( segmentDataFull.isNullOrEmpty() ) null else getSegmentData(el, segmentDataFull)

                StringUtils.normalizeString(el.name) to segmentData 
            }.toMap() // .mmgPid.map


            //  ------------- OBR
            // ----------------------------------------------------
            val mmgObr = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "OBR" }

            val obrMap = mmgObr.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition

                val obrLineParts = messageLines.filter { it.startsWith("OBR|") && it.contains(OBR_4_1_EPI_ID) }[0].split("|") // there would be only one Epi Obr

                val segmentDataFull = if (obrLineParts.size > dataFieldPosition) obrLineParts[dataFieldPosition].trim() else null  

                val segmentData = if ( segmentDataFull.isNullOrEmpty() ) null else getSegmentData(el, segmentDataFull)

                StringUtils.normalizeString(el.name) to segmentData 
            }.toMap() // .mmgObr.map

            
            //  ------------- OBX
            // ----------------------------------------------------
            val mmgObx = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "OBX" }

            val obxLines = messageLines.filter { it.startsWith("OBX|") }

            val obxMap = mmgObx.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition

                val mmgObxIdentifier = el.mappings.hl7v251.identifier

                val obxLine = obxLines.filter { line -> 
                    val lineParts = line.split("|")
                    val obxIdentifier = lineParts[3].split("^")[0]
                    mmgObxIdentifier == obxIdentifier
                } // .obxLine
                
                val obxLineParts = if (obxLine.size > 0) obxLine[0].split("|") else listOf<String>()
                
                val segmentDataFull = if (obxLineParts.size > dataFieldPosition) obxLineParts[dataFieldPosition] else null 

                val segmentData = if ( segmentDataFull.isNullOrEmpty() ) null else getSegmentData(el, segmentDataFull)

                StringUtils.normalizeString(el.name) to segmentData
            }.toMap() // .mmgPid.map

            
            val mmgModelBlocksSingle = mshMap + pidMap + obrMap + obxMap
            
            // logger.info("mmgModelBlocksSingle.size: --> ${mmgModelBlocksSingle.size}\n")
            // logger.info("mmgElemsBlocksSingle.size: --> ${mmgElemsBlocksSingle.size}\n")

            // logger.info("MMG Model (mmgModelBlocksSingle): --> ${gsonWithNullsOn.toJson(mmgModelBlocksSingle)}\n")
            return mmgModelBlocksSingle
        } // .hl7ToJsonModelBlocksSingle 
        

        // --------------------------------------------------------------------------------------------------------
        //  ------------- hl7ToJsonModelBlocksNonSingle ------------- BLOCKS NON SINGLE
        // --------------------------------------------------------------------------------------------------------

        //? @Throws(Exception::class) 
        fun hl7ToJsonModelBlocksNonSingle(hl7Content: String, mmgsArr: Array<MMG>): Map<String, List<Map<String, Any?>>> {
            // val jedis = redisProxy.getJedisClient()
            // there could be multiple MMGs each with MSH, PID -> filter out and only keep the one's from the last MMG 
            val mmgs = getMmgsFiltered(mmgsArr)
 
            val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks

            val obxIdToElementMap = getObxIdToElementMap(mmgBlocks)

            val (_, mmgBlocksNonSingle) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }

            val messageLines = getMessageLines(hl7Content)

            val obxLines = messageLines.filter { it.startsWith("OBX|") }

            val blocksNonSingleModel = mmgBlocksNonSingle.map{ block -> 

                val msgLines = block.elements.flatMap { element -> 

                    // logger.info("element: --> ${element.mappings.hl7v251.identifier}\n")
                    val mmgObxIdentifier = element.mappings.hl7v251.identifier

                    val obxLinesForElem = obxLines.filter { line -> 
                        val lineParts = line.split("|")
                        val obxIdentifier = lineParts[3].split("^")[0]
                        mmgObxIdentifier == obxIdentifier
                    } // .obxLine
                    
                    obxLinesForElem
                } // .block.elements

                // logger.info("block.name: ${block.name}, msgLines.size: ${msgLines.size}")

                val msgLinesByBlockNumMap = msgLines.map { line -> 
                    val lineParts = line.split("|")
                    val blockNum = lineParts[4]
                    blockNum to line
                }.groupBy( { it.first }, { it.second } )

                // logger.info("msgLinesByBlockNumMap.size: ${msgLinesByBlockNumMap.size}")

                val blockElementsNameDataTupMap = msgLinesByBlockNumMap.map { (_, lines) -> 
                    val mapFromMsg = lines.map { line -> 
                        val lineParts = line.split("|")
                        val dataFieldPosition = 5 //element.mappings.hl7v251.fieldPosition

                        val segmentDataFull = if (lineParts.size > dataFieldPosition) lineParts[dataFieldPosition].trim() else null

                        val obxIdentifier = lineParts[3].split("^")[0]

                        // if ( obxIdToElementMap.contains(obxIdentifier) ) {}

                        val el = obxIdToElementMap[obxIdentifier]!! // this obxIdentifier is in the map see line 182, 184 filter

                        val segmentData = if ( segmentDataFull.isNullOrEmpty() ) null else getSegmentData(el, segmentDataFull)

                        StringUtils.normalizeString(el.name) to segmentData
                    }.toMap() // .lines

                    // add block elements that are not found in the message lines
                    val elemsNotInLines = block.elements.filter{ elemx -> 

                        val mmgObxId = elemx.mappings.hl7v251.identifier 

                        val linesForObxId = lines.filter{ linex -> 
                            val linePartsx = linex.split("|")
                            val obxId = linePartsx[3].split("^")[0]
                            mmgObxId == obxId
                        }
                        linesForObxId.size == 0
                    } // .elemsNotInLines

                    val mapFromElemNotInMsg = elemsNotInLines.map{elem ->
                        StringUtils.normalizeString(elem.name) to null 
                    }.toMap()

                    mapFromMsg + mapFromElemNotInMsg
                } // .blockElementsNameDataTupMap
                // logger.info("\nblockElementsNameDataTupMap: --> ${Gson().toJson(blockElementsNameDataTupMap)}\n\n")

                StringUtils.normalizeString(block.name) to blockElementsNameDataTupMap
            }.toMap() // .blocksNonSingleModel

            
            // logger.info("blocksNonSingleModel.size: --> ${blocksNonSingleModel.size}\n")
            // logger.info("MMG Model (blocksNonSingleModel): --> ${gsonWithNullsOn.toJson(blocksNonSingleModel)}\n")
            return blocksNonSingleModel
        } // .hl7ToJsonModelBlocksNonSingle 
        


        // --------------------------------------------------------------------------------------------------------
        //  ------------- Functions used in the transformation -------------
        // --------------------------------------------------------------------------------------------------------

        private fun getMessageLines(hl7Content: String): List<String> {

            return hl7Content.split("\n")
        } // .getMessageLines


        private fun getObxIdToElementMap(blocks: List<Block>): Map<String, Element> {

            val elems = blocks.flatMap { it.elements } // .mmgElemsBlocksSingle

            return elems.map{ elem -> 
                elem.mappings.hl7v251.identifier to elem
            }.toMap()
        } // .getObxIdToElementMap


        /* private */ fun getMmgsFiltered(mmgs: Array<MMG>): Array<MMG> {

            if ( mmgs.size > 1 ) { 
                for ( index in 0..mmgs.size - 2) { // except the last one
                    mmgs[index].blocks = mmgs[index].blocks.filter { block ->
                        !( block.name == MMG_BLOCK_NAME_MESSAGE_HEADER ) //|| block.name == MMG_BLOCK_NAME_SUBJECT_RELATED )
                    } // .filter
                } // .for
            } // .if

            return mmgs
        } // .getMmgsFiltered

        
        /* private */ fun getPhinDataTypes(): Map<String, List<PhinDataType>> {
            // logger.info("getPhinDataTypes, reading local file...")

            val dataTypesFilePath = "/DefaultFieldsProfileX.json"
            val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath).readText()
            // val dataTypesMapJson = this::class.java.classLoader.getResource(dataTypesFilePath).readText()

            // val dataTypesMap = Map<String, List<PhinDataType>>
            val dataTypesMapType = object : TypeToken< Map<String, List<PhinDataType>> >() {}.type

            val profilesMap: Map<String, List<PhinDataType>> = gson.fromJson(dataTypesMapJson, dataTypesMapType)

            // logger.info("getPhinDataTypes, profilesMap: --> $profilesMap")
            return profilesMap
        } // .getPhinDataTypes


        /* private */ fun getSegmentData(el: Element, segmentDataFull: String): Any? {
            // logger.info("getSegmentData, calling getPhinDataTypes...")

            val phinDataTypesMap = getPhinDataTypes()
            // logger.info("getSegmentData, phinDataTypesMap: --> ${phinDataTypesMap}")
            
            val segmentDataArr = if (el.isRepeat) segmentDataFull.split("~") else listOf(segmentDataFull)

            val segmentData = segmentDataArr.map { oneRepeat ->
                val oneRepeatParts = oneRepeat.split("^")

                if ( phinDataTypesMap.contains(el.mappings.hl7v251.dataType) ) {

                    val map1 = phinDataTypesMap[el.mappings.hl7v251.dataType]!!.map { phinDataTypeEntry -> 

                        val fieldNumber = phinDataTypeEntry.fieldNumber.toInt() - 1

                        val dt = if (oneRepeatParts.size > fieldNumber && oneRepeatParts[fieldNumber].length > 0) oneRepeatParts[fieldNumber] else null
                        StringUtils.normalizeString(phinDataTypeEntry.name) to dt
                    }.toMap()
                    
                    // val map2 = mapOf( PHIN_DATA_TYPE_KEY_NAME to el.mappings.hl7v251.dataType)

                    // call vocab for preferred name and cdc preffered name
                    if ( el.mappings.hl7v251.dataType == ELEMENT_CE || el.mappings.hl7v251.dataType == ELEMENT_CWE ) {
                        // both CE and CWE 

                            val valueSetCode = el.valueSetCode// "PHVS_YesNoUnknown_CDC" // "PHVS_ClinicalManifestations_Lyme"
                            val conceptCode = map1["identifier"]
                            
                            val conceptJson = redisClient.hget(REDIS_VOCAB_NAMESPACE + valueSetCode, conceptCode)


                            map1 /*+ map2 */+ if ( conceptJson.isNullOrEmpty() ) { // map2 used for dev only

                                // No Redis entry!! for this value set code, concept code
                                mapOf(
                                    CODE_SYSTEM_CONCEPT_NAME_KEY_NAME to null,
                                    CDC_PREFERRED_DESIGNATION_KEY_NAME to null ) 
                            } else { // 

                                // logger.info("ValueSetConcept conceptJson: --> $conceptJson")
                                val cobj:ValueSetConcept = gson.fromJson(conceptJson, ValueSetConcept::class.java)

                                mapOf(
                                   CODE_SYSTEM_CONCEPT_NAME_KEY_NAME to cobj.codeSystemConceptName,
                                   CDC_PREFERRED_DESIGNATION_KEY_NAME to cobj.cdcPreferredDesignation )
                            } // .else 

                        } else {
                            // this is not an ELEMENT_CE || ELEMENT_CWE
                            map1 //+ map2 
                        } // .else

                } else {
                    // not available in the default fields profile (DefaultFieldsProfile.json)
                    // considering the component position
                    // dtCompPos data component position
                    val dtCompPos = el.mappings.hl7v251.componentPosition - 1

                    when ( dtCompPos ) {
                        in 0..Int.MAX_VALUE ->  if ( oneRepeatParts.size > dtCompPos && oneRepeatParts[dtCompPos].length > 0 ) oneRepeatParts[dtCompPos] else null
                        // no need to use component position
                        else -> oneRepeat // full data (string)
                    } // .when
                    
                } // .else

            } // .segmentData 

            return if (el.isRepeat) segmentData else segmentData[0]
        } // .getSegmentData

    // } // .companion object
} // .Transformer

