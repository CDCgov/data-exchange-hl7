package gov.cdc.dex.hl7

// import redis.clients.jedis.DefaultJedisClientConfig
// import redis.clients.jedis.Jedis

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.model.PhinDataType
import gov.cdc.dex.redisModels.Block
import gov.cdc.dex.redisModels.Element
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.redisModels.ValueSetConcept
import gov.cdc.dex.util.StringUtils

class Transformer(redisProxy: RedisProxy)  {

    private val redisClient = redisProxy.getJedisClient()

    companion object {
      //  val logger: Logger = LoggerFactory.getLogger(Transformer::class.java.simpleName)
        private val gson = Gson()

        const val MMG_BLOCK_TYPE_SINGLE = "Single"
        private const val OBR_4_1_EPI_ID = "68991-9"
        private const val OBR_4_1_LEGACY = "PERSUBJ"
        private const val MMG_BLOCK_NAME_MESSAGE_HEADER = "Message Header"
        // private val MMG_BLOCK_NAME_SUBJECT_RELATED = "Subject Related"
        private const val REDIS_VOCAB_NAMESPACE = "vocab:"
        private const val ELEMENT_CE = "CE"
        private const val ELEMENT_CWE = "CWE"
        // private val PHIN_DATA_TYPE_KEY_NAME = "phin_data_type" // only used in dev
        private const val CODE_SYSTEM_CONCEPT_NAME_KEY_NAME = "code_system_concept_name"
        private const val CDC_PREFERRED_DESIGNATION_KEY_NAME =  "cdc_preferred_designation"
        private const val MAX_BLOCK_NAME_LENGTH = 30
    }

        // --------------------------------------------------------------------------------------------------------
        //  ------------- hl7ToJsonModelBlocksSingle ------------- BLOCKS SINGLE
        // --------------------------------------------------------------------------------------------------------

        //? @Throws(Exception::class)
        fun hl7ToJsonModelBlocksSingle(hl7Content: String, mmgsArr: Array<MMG>): Map<String, Any?> {

            // there could be multiple MMGs each with MSH -> filter out MSH block from all except last MMG listed
            val mmgs = getMmgsFiltered(mmgsArr)

            val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks

            val (mmgBlocksSingle, _) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }

            val messageLines = getMessageLines(hl7Content)

            val mmgElemsBlocksSingle = mmgBlocksSingle.flatMap { it.elements } // .mmgElemsBlocksSingle
            //logger.info("mmgElemsBlocksSingle.size: --> ${mmgElemsBlocksSingle.size}")

            //  ------------- MSH
            // ----------------------------------------------------
            val mmgMsh = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "MSH" }
            //logger.info("Mapping MSH")
            val mshMap = mmgMsh.associate { el ->
                val dataFieldPosition = el.mappings.hl7v251.fieldPosition - 1
                val mshLineParts =
                    messageLines.filter { it.startsWith("MSH|") }[0].split("|") // there would be only one MSH
                val segmentData = getSegmentData(mshLineParts, dataFieldPosition, el)
                StringUtils.normalizeString(el.name) to segmentData
            } // .mmgMsh.map


            //  ------------- PID
            // ----------------------------------------------------
            val mmgPid = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "PID" }
            //logger.info("Mapping PID")
            val pidMap = mmgPid.associate { el ->
                val pidLineParts =
                    messageLines.filter { it.startsWith("PID|") }[0].split("|") // there would be only one PID
                val segmentData = getSegmentData(pidLineParts, el.mappings.hl7v251.fieldPosition, el)
                StringUtils.normalizeString(el.name) to segmentData
            } // .mmgPid.map

            //  ------------- OBR
            // ----------------------------------------------------
            val mmgObr = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "OBR" }
            //logger.info("Mapping OBR")
            val obrMap = mmgObr.associate { el ->
                val obrLineParts =
                    messageLines.filter { it.startsWith("OBR|") && (it.contains(OBR_4_1_EPI_ID) || it.contains(
                        OBR_4_1_LEGACY)) }[0].split("|") // there would be only one Epi Obr
                val segmentData = getSegmentData(obrLineParts, el.mappings.hl7v251.fieldPosition, el)
                StringUtils.normalizeString(el.name) to segmentData
            } // .mmgObr.map

            //  ------------- OBX
            // ----------------------------------------------------
            val mmgObx = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "OBX" }
            //logger.info("Mapping OBX")
            val obxLines = messageLines.filter { it.startsWith("OBX|") }
            val obxMap = mmgObx.associate { el ->
                val obxLine = filterByIdentifier(obxLines, el.mappings.hl7v251.identifier)
                val obxLineParts = if (obxLine.isNotEmpty()) obxLine[0].split("|") else listOf()
                val segmentData = getSegmentData(obxLineParts, el.mappings.hl7v251.fieldPosition, el)
                if (el.isRepeat) {
                    StringUtils.getNormalizedShortName(el.name, MAX_BLOCK_NAME_LENGTH) to segmentData
                } else {
                    StringUtils.normalizeString(el.name) to segmentData
                }
            } // .mmgPid.map


            val mmgModelBlocksSingle = mshMap + pidMap + obrMap + obxMap

             //logger.info("mmgModelBlocksSingle.size: --> ${mmgModelBlocksSingle.size}\n")
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

            val blocksNonSingleModel = mmgBlocksNonSingle.associate { block ->

                val msgLines = block.elements.flatMap { element ->
                    // logger.info("element: --> ${element.mappings.hl7v251.identifier}\n")
                    filterByIdentifier(obxLines, element.mappings.hl7v251.identifier)
                } // .block.elements

                // logger.info("block.name: ${block.name}, msgLines.size: ${msgLines.size}")

                val msgLinesByBlockNumMap = msgLines.map { line ->
                    val lineParts = line.split("|")
                    val blockNum = lineParts[4]
                    blockNum to line
                }.groupBy({ it.first }, { it.second })

                // logger.info("msgLinesByBlockNumMap.size: ${msgLinesByBlockNumMap.size}")

                val blockElementsNameDataTupMap = msgLinesByBlockNumMap.map { (_, lines) ->
                    val mapFromMsg = lines.associate { line ->
                        val lineParts = line.split("|")
                        val dataFieldPosition = 5 //element.mappings.hl7v251.fieldPosition
                        val obxIdentifier = lineParts[3].split("^")[0]
                        // if ( obxIdToElementMap.contains(obxIdentifier) ) {}
                        val el =
                            obxIdToElementMap[obxIdentifier]!! // this obxIdentifier is in the map
                        val segmentData = getSegmentData(lineParts, dataFieldPosition, el)
                        StringUtils.normalizeString(el.name) to segmentData
                    } // .lines

                    // add block elements that are not found in the message lines
                    val elemsNotInLines = block.elements.filter { elemx ->
                        val linesForObxId = filterByIdentifier(lines, elemx.mappings.hl7v251.identifier)
                        linesForObxId.isEmpty()
                    } // .elemsNotInLines

                    val mapFromElemNotInMsg = elemsNotInLines.associate { elem ->
                        StringUtils.normalizeString(elem.name) to null
                    }

                    mapFromMsg + mapFromElemNotInMsg
                } // .blockElementsNameDataTupMap
                // logger.info("\nblockElementsNameDataTupMap: --> ${Gson().toJson(blockElementsNameDataTupMap)}\n\n")

                StringUtils.getNormalizedShortName(block.name, MAX_BLOCK_NAME_LENGTH) to blockElementsNameDataTupMap
            } // .blocksNonSingleModel

            
            // logger.info("blocksNonSingleModel.size: --> ${blocksNonSingleModel.size}\n")
            // logger.info("MMG Model (blocksNonSingleModel): --> ${gsonWithNullsOn.toJson(blocksNonSingleModel)}\n")
            return blocksNonSingleModel
        } // .hl7ToJsonModelBlocksNonSingle 
        


        // --------------------------------------------------------------------------------------------------------
        //  ------------- Functions used in the transformation -------------
        // --------------------------------------------------------------------------------------------------------

           private fun filterByIdentifier(lines: List<String>, id: String) : List<String> {
            return lines.filter { line ->
                val lineParts = line.split("|")
                val obxId = lineParts[3].split("^")[0]
                id == obxId
            }
        }
        private fun getMessageLines(hl7Content: String): List<String> {

            return hl7Content.split("\n")
        } // .getMessageLines


        private fun getObxIdToElementMap(blocks: List<Block>): Map<String, Element> {

            val elems = blocks.flatMap { it.elements } // .mmgElemsBlocksSingle

            return elems.associateBy { elem ->
                elem.mappings.hl7v251.identifier
            }
        } // .getObxIdToElementMap


        /* private */ fun getMmgsFiltered(mmgs: Array<MMG>): Array<MMG> {

            if ( mmgs.size > 1 ) { 
                for ( index in 0..mmgs.size - 2) { // except the last one
                    mmgs[index].blocks = mmgs[index].blocks.filter { block ->
                        block.name != MMG_BLOCK_NAME_MESSAGE_HEADER //|| block.name == MMG_BLOCK_NAME_SUBJECT_RELATED
                    } // .filter
                } // .for
            } // .if

            return mmgs
        } // .getMmgsFiltered

        
        /* private */ fun getPhinDataTypes(): Map<String, List<PhinDataType>> {
            // logger.info("getPhinDataTypes, reading local file...")

            val dataTypesFilePath = "/DefaultFieldsProfileX.json"
            val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath)?.readText()
            // val dataTypesMapJson = this::class.java.classLoader.getResource(dataTypesFilePath).readText()

            // val dataTypesMap = Map<String, List<PhinDataType>>
            val dataTypesMapType = object : TypeToken< Map<String, List<PhinDataType>> >() {}.type

            val profilesMap: Map<String, List<PhinDataType>> = gson.fromJson(dataTypesMapJson, dataTypesMapType)

            // logger.info("getPhinDataTypes, profilesMap: --> $profilesMap")
            return profilesMap
        } // .getPhinDataTypes


        /* private */ private fun getSegmentData(lineParts: List<String>, dataFieldPosition: Int, el: Element): Any? {
            // logger.info("getSegmentData, calling getPhinDataTypes...")
            if (lineParts.size > dataFieldPosition) {
                val segmentDataFull = lineParts[dataFieldPosition].trim()
                val phinDataTypesMap = getPhinDataTypes()
                // logger.info("getSegmentData, phinDataTypesMap: --> ${phinDataTypesMap}")
                val segmentDataArr = if (el.isRepeat) segmentDataFull.split("~") else listOf(segmentDataFull)
                val segmentData = segmentDataArr.map { oneRepeat ->
                    val oneRepeatParts = oneRepeat.split("^")
                    if ( phinDataTypesMap.contains(el.mappings.hl7v251.dataType) ) {
                        val map1 = phinDataTypesMap[el.mappings.hl7v251.dataType]!!.associate {
                                phinDataTypeEntry ->
                            val fieldNumber = phinDataTypeEntry.fieldNumber.toInt() - 1
                            val dt =
                                if (oneRepeatParts.size > fieldNumber && oneRepeatParts[fieldNumber].isNotEmpty()) oneRepeatParts[fieldNumber] else null
                            StringUtils.normalizeString(phinDataTypeEntry.name) to dt
                        }
                        // call vocab for preferred name and cdc preferred name
                        if ( el.mappings.hl7v251.dataType == ELEMENT_CE || el.mappings.hl7v251.dataType == ELEMENT_CWE ) {
                            // both CE and CWE
                            val valueSetCode = el.valueSetCode// "PHVS_YesNoUnknown_CDC" // "PHVS_ClinicalManifestations_Lyme"
                            val conceptCode = map1["identifier"]
                            var conceptJson = ""
                            if ((!valueSetCode.isNullOrEmpty() && valueSetCode != "N/A") && !conceptCode.isNullOrEmpty()) {
                                conceptJson = redisClient.hget(REDIS_VOCAB_NAMESPACE + valueSetCode, conceptCode)
                            }
                            map1 /*+ map2 */ + if (conceptJson.isEmpty()) { // map2 used for dev only
                                // No Redis entry!! for this value set code, concept code
                                mapOf(
                                    CODE_SYSTEM_CONCEPT_NAME_KEY_NAME to null,
                                    CDC_PREFERRED_DESIGNATION_KEY_NAME to null
                                )
                            } else { //
                                // logger.info("ValueSetConcept conceptJson: --> $conceptJson")
                                val cobj: ValueSetConcept = gson.fromJson(conceptJson, ValueSetConcept::class.java)
                                mapOf(
                                    CODE_SYSTEM_CONCEPT_NAME_KEY_NAME to cobj.codeSystemConceptName,
                                    CDC_PREFERRED_DESIGNATION_KEY_NAME to cobj.cdcPreferredDesignation
                                )
                            } // .else
                        } else {
                            // this is not an ELEMENT_CE || ELEMENT_CWE
                            map1 //+ map2
                        } // .else
                    } else {
                        // not available in the default fields profile (DefaultFieldsProfile.json)
                        // considering the component position
                        // dtCompPos data component position
                        when (val dtCompPos = el.mappings.hl7v251.componentPosition - 1) {
                            in 0..Int.MAX_VALUE ->  if ( oneRepeatParts.size > dtCompPos && oneRepeatParts[dtCompPos].isNotEmpty()) oneRepeatParts[dtCompPos] else null
                            // no need to use component position
                            else -> oneRepeat // full data (string)
                        } // .when

                    } // .else

                } // .segmentData
                return if (el.isRepeat) segmentData else segmentData[0]
            } else {
                return null
            }
        } // .getSegmentData

    // } // .companion object
} // .Transformer

