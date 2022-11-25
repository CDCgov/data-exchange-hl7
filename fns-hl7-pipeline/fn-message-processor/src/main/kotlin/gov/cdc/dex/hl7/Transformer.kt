package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.redisModels.Block
import gov.cdc.dex.redisModels.Element
import gov.cdc.dex.redisModels.ValueSetConcept

import gov.cdc.dex.hl7.model.PhinDataType

import gov.cdc.dex.util.StringUtils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

class Transformer  {

    companion object {
        val logger = LoggerFactory.getLogger(Transformer::class.java.simpleName)
        private val gson = Gson()

        val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")

        val jedis = Jedis(REDIS_CACHE_NAME, 6380, DefaultJedisClientConfig.builder()
        .password(REDIS_PWD)
        .ssl(true)
        .build())

        const val MMG_BLOCK_TYPE_SINGLE = "Single"
        private val OBR_4_1_EPI_ID = "68991-9"
        private val MMG_BLOCK_NAME_MESSAGE_HEADER = "Message Header" 
        // private val MMG_BLOCK_NAME_SUBJECT_RELATED = "Subject Related"
        private val REDIS_VOCAB_NAMESPACE = "vocab:"
        private val ELEMENT_CE = "CE"
        private val ELEMENT_CWE = "CWE"
        private val PHIN_DATA_TYPE_KEY_NAME = "phin_data_type"
        private val CODE_SYSTEM_CONCEPT_NAME_KEY_NAME = "code_system_concept_name"
        private val CDC_PREFERRED_DESIGNATION_KEY_NAME =  "cdc_preferred_designation"

        // --------------------------------------------------------------------------------------------------------
        //  ------------- hl7ToJsonModelBlocksSingle ------------- BLOCKS SINGLE
        // --------------------------------------------------------------------------------------------------------
        
        //? @Throws(Exception::class) 
        fun hl7ToJsonModelBlocksSingle(hl7Content: String, mmgsArr: Array<MMG>): Map<String, Any?> {

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
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 

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
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 

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
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 

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
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 
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
            
            logger.info("mmgModelBlocksSingle.size: --> ${mmgModelBlocksSingle.size}\n")
            logger.info("MMG Model (mmgModelBlocksSingle): --> ${Gson().toJson(mmgModelBlocksSingle)}\n")
            return mmgModelBlocksSingle
        } // .hl7ToJsonModelBlocksSingle 
        

        // --------------------------------------------------------------------------------------------------------
        //  ------------- hl7ToJsonModelBlocksNonSingle ------------- BLOCKS NON SINGLE
        // --------------------------------------------------------------------------------------------------------
        //? @Throws(Exception::class) 
        fun hl7ToJsonModelBlocksNonSingle(hl7Content: String, mmgsArr: Array<MMG>): Map<String, List<Map<String, Any?>>> {

            // there could be multiple MMGs each with MSH, PID -> filter out and only keep the one's from the last MMG 
            val mmgs = getMmgsFiltered(mmgsArr)
 
            val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks

            val obxIdToElementMap = getObxIdToElementMap(mmgBlocks)

            val (_, mmgBlocksNonSingle) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }

            val messageLines = getMessageLines(hl7Content)

            val obxLines = messageLines.filter { it.startsWith("OBX|") }

            val obxIdToElNameMap = getObxIdToElNameMap(mmgBlocksNonSingle)

            val blocksNonSingleModel = mmgBlocksNonSingle.map{ block -> 

                val msgLines = block.elements.flatMap { element -> 

                    // logger.info("element: --> ${element.mappings.hl7v251.identifier}\n")
                    val mmgObxIdentifier = element.mappings.hl7v251.identifier

                    obxLines.filter { line -> 
                        val lineParts = line.split("|")
                        val obxIdentifier = lineParts[3].split("^")[0]
                        mmgObxIdentifier == obxIdentifier
                    } // .obxLine
                    // logger.info("elemLines: --> ${elemLines}\n")

                    // return elemLines
                } // .block.elements

                val msgLinesByBlockNumMap = msgLines.map { line -> 
                    val lineParts = line.split("|")
                    val blockNum = lineParts[4]
                    blockNum to line
                }.groupBy( { it.first }, { it.second } )

                val blockElementsNameDataTupMap = msgLinesByBlockNumMap.map { (_, lines) -> 
                    lines.map { line -> 
                        val lineParts = line.split("|")
                        val dataFieldPosition = 5 //element.mappings.hl7v251.fieldPosition

                        val segmentDataFull = if (lineParts.size > dataFieldPosition) lineParts[dataFieldPosition].trim() else null

                        val obxIdentifier = lineParts[3].split("^")[0]

                        if ( obxIdToElementMap.contains(obxIdentifier) ) {

                            val el = obxIdToElementMap[obxIdentifier]!!

                            val segmentData = if ( segmentDataFull.isNullOrEmpty() ) null else getSegmentData(el, segmentDataFull)

                            StringUtils.normalizeString(el.name) to segmentData

                        } else {
                            // should not happen, this obx identifier is unknown to the MMGs
                            // TODO: throw exception?
                            "unknown_segment_identifier" to segmentDataFull
                        }

                        val segmentData = if ( segmentDataFull.isNullOrEmpty() ) null else getSegmentData(obxIdToElementMap[obxIdentifier]!!, segmentDataFull)

                        val elName = obxIdToElNameMap.getOrElse(obxIdentifier) { "TODO:throw_error?" }

                        StringUtils.normalizeString(elName) to segmentData
                    }.toMap() // .lines
                } // .blockElementsNameDataTupMap
                // logger.info("\nblockElementsNameDataTupMap: --> ${Gson().toJson(blockElementsNameDataTupMap)}\n\n")

                StringUtils.normalizeString(block.name) to blockElementsNameDataTupMap
            }.toMap() // .blocksNonSingleModel

            
            logger.info("blocksNonSingleModel.size: --> ${blocksNonSingleModel.size}\n")
            logger.info("MMG Model (blocksNonSingleModel): --> ${Gson().toJson(blocksNonSingleModel)}\n")
            return blocksNonSingleModel
        } // .hl7ToJsonModelBlocksNonSingle 
        


        // --------------------------------------------------------------------------------------------------------
        //  ------------- Functions used in the transformation
        // --------------------------------------------------------------------------------------------------------

        private fun getMessageLines(hl7Content: String): List<String> {

            return hl7Content.split("\n")
        } // .getMessageLines


        private fun getObxIdToElNameMap(blocks: List<Block>): Map<String, String> {

            val elems = blocks.flatMap { it.elements } // .mmgElemsBlocksSingle

            return elems.map{ elem -> 
                elem.mappings.hl7v251.identifier to elem.name
            }.toMap()
        } // .getObxIdToElNameMap

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

            val dataTypesFilePath = "/DefaultFieldsProfile.json"
            val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath).readText()

            // val dataTypesMap = Map<String, List<PhinDataType>>
            val dataTypesMapType = object : TypeToken< Map<String, List<PhinDataType>> >() {}.type

            return gson.fromJson(dataTypesMapJson, dataTypesMapType)
        } // .getPhinDataTypes


        /* private */ fun getSegmentData(el: Element, segmentDataFull: String): Any {
            
            val phinDataTypesMap = getPhinDataTypes()

            val segmentDataArr = if (el.isRepeat) segmentDataFull.split("~") else listOf(segmentDataFull)

            val segmentData = segmentDataArr.map { oneRepeat ->
                val oneRepeatParts = oneRepeat.split("^")

                if ( phinDataTypesMap.contains(el.mappings.hl7v251.dataType) ) {

                    val map1 = phinDataTypesMap[el.mappings.hl7v251.dataType]!!.map { phinDataTypeEntry -> 

                        val fieldNumber = phinDataTypeEntry.fieldNumber.toInt() - 1

                        val dt = if (oneRepeatParts.size > fieldNumber) oneRepeatParts[fieldNumber] else null
                        StringUtils.normalizeString(phinDataTypeEntry.name) to dt
                    }.toMap()
                    
                    val map2 = mapOf( PHIN_DATA_TYPE_KEY_NAME to el.mappings.hl7v251.dataType)

                    // call vocab for preferred name and cdc preffered name
                    if ( el.mappings.hl7v251.dataType == ELEMENT_CE || el.mappings.hl7v251.dataType == ELEMENT_CWE ) {
                        // TODO: check if both CE and CWE !!

                            val valueSetCode = el.valueSetCode// "PHVS_YesNoUnknown_CDC" // "PHVS_ClinicalManifestations_Lyme"
                            val conceptCode = map1["text"]
                            
                            val conceptJson = jedis.hget(REDIS_VOCAB_NAMESPACE + valueSetCode, conceptCode)

                            map1 + map2 + if ( conceptJson.isNullOrEmpty() ) {
                                // logger.info("conceptJson: isNullOrEmpty --> $conceptJson")

                                // No Redis entry!! for this value set code, concept code
                                mapOf(
                                    CODE_SYSTEM_CONCEPT_NAME_KEY_NAME to null,
                                    CDC_PREFERRED_DESIGNATION_KEY_NAME to null ) 
                            } else { // 
                                // logger.info("conceptJson: --> $conceptJson")
                                val cobj:ValueSetConcept = gson.fromJson(conceptJson, ValueSetConcept::class.java)
                                mapOf(
                                   CODE_SYSTEM_CONCEPT_NAME_KEY_NAME to cobj.codeSystemConceptName,
                                   CODE_SYSTEM_CONCEPT_NAME_KEY_NAME to cobj.cdcPreferredDesignation )
                            } // .else 

                        } else {
                            // this is not an ELEMENT_CE || ELEMENT_CWE
                            map1 + map2 
                        } // .else

                } else {
                    // not available in the default fields profile (DefaultFieldsProfile.json)
                    // returns segment data as is in hl7 message
                    oneRepeat
                } // .else

            } // .segmentData 

            return if (el.isRepeat) segmentData else segmentData[0]
        } // .getSegmentData

    } // .companion object
} // .Transformer

