package gov.cdc.dex.hl7

// import redis.clients.jedis.DefaultJedisClientConfig
// import redis.clients.jedis.Jedis

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.model.PhinDataType
import gov.cdc.dex.redisModels.Element
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.redisModels.ValueSetConcept
import gov.cdc.dex.util.StringUtils
import gov.cdc.dex.util.StringUtils.Companion.normalize
import gov.cdc.dex.TemplateTransformer
import gov.cdc.hl7.HL7StaticParser

class Transformer(redisProxy: RedisProxy)  {

    private val redisClient = redisProxy.getJedisClient()

    companion object {
      //  val logger: Logger = LoggerFactory.getLogger(Transformer::class.java.simpleName)
        val gson: Gson = GsonBuilder().serializeNulls().create()

        const val MMG_BLOCK_TYPE_SINGLE = "Single"
        private const val OBR_4_1_EPI_ID = "68991-9"
        private const val OBR_4_1_LEGACY = "NOTF"
        private const val OBR_4_1_SUBJECT = "PERSUBJ"
        private const val MMG_BLOCK_NAME_MESSAGE_HEADER = "Message Header"
        private const val REDIS_VOCAB_NAMESPACE = "vocab:"
        private const val ELEMENT_CE = "CE"
        private const val ELEMENT_CWE = "CWE"
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
            // get only the EPI OBXs
            val obxLines = getEpiOBXs(messageLines)
            val obxMap = mmgObx.associate { el ->
                val obxLine = filterByIdentifier(obxLines, el.mappings.hl7v251.identifier)
                val obxLineParts = if (obxLine.isNotEmpty()) obxLine[0].split("|") else listOf()
                val segmentData = getSegmentData(obxLineParts, el.mappings.hl7v251.fieldPosition, el)
                if (el.isRepeat || el.mayRepeat.contains("Y")) {
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
            val (_, mmgBlocksNonSingle) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }

            val messageLines = getMessageLines(hl7Content)
            // get only the EPI OBXs
            val obxLines = getEpiOBXs(messageLines)

            val blocksNonSingleModel = mmgBlocksNonSingle.associate { block ->
                val obxIdToElementMap = block.elements.associateBy { element ->  element.mappings.hl7v251.identifier }
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
                // make sure the block is properly identified as a repeating block
                val blockName = if (block.name.normalize().contains("repeating_group")) {
                    block.name
                } else {
                    "${block.name} repeating group"
                }
                StringUtils.getNormalizedShortName(blockName, MAX_BLOCK_NAME_LENGTH) to blockElementsNameDataTupMap
            } // .blocksNonSingleModel

            
            // logger.info("blocksNonSingleModel.size: --> ${blocksNonSingleModel.size}\n")
            // logger.info("MMG Model (blocksNonSingleModel): --> ${gsonWithNullsOn.toJson(blocksNonSingleModel)}\n")
            return blocksNonSingleModel
        } // .hl7ToJsonModelBlocksNonSingle 
        
        fun hl7ToJsonModelLabTemplate(hl7Content: String) : Map<String, List<Map<String, Any?>>>{
            val messageLines = getMessageLines(hl7Content)
            val nonEpiOBRs = getNonEpiOBRs(messageLines)
            val bumblebee = TemplateTransformer.getTransformerWithResource("/labTemplate.json", "/BasicProfile.json")

            nonEpiOBRs.forEach { obr ->
                val identifier = obr.split("|")[4].split("^")[0]
                val labSegments = getGroupSegmentsForThisOBR(messageLines, identifier)
                if (labSegments.isNotEmpty()) {
                    val labData = labSegments.joinToString("\n")
                    val labJson = bumblebee.transformMessage(labData)
                    //TODO: put in format to return to main function
                }
            }

        }

        // --------------------------------------------------------------------------------------------------------
        //  ------------- Functions used in the transformation -------------
        // --------------------------------------------------------------------------------------------------------

        fun getEpiOBXs(hl7MessageLines: List<String>): List<String> {
            // get the OBX lines from the message that come after the EPI OBR
            // up to the point where another OBR occurs.
            val epiIndex = hl7MessageLines.indexOfFirst { line -> line.startsWith("OBR|")
                    && line.split("|")[4].split("^")[0].trim() in listOf(
                OBR_4_1_EPI_ID, OBR_4_1_LEGACY) }

            val firstNonEpi = hl7MessageLines.withIndex().indexOfFirst { (index, line) ->
                index > epiIndex
                        && line.startsWith("OBR|")
                        && line.split("|")[4].split("^")[0].trim() !in listOf(
                            OBR_4_1_EPI_ID, OBR_4_1_LEGACY, OBR_4_1_SUBJECT)
            }

            return if (firstNonEpi == -1) {
                val end = hl7MessageLines.subList(epiIndex + 1, hl7MessageLines.size)
                end.filter { it.startsWith("OBX|") }
            } else {
                hl7MessageLines.subList(epiIndex + 1, firstNonEpi)
            }
        }

        fun getNonEpiOBRs(hl7MessageLines: List<String>) : List<String> {
            return hl7MessageLines.filter { line ->
                line.startsWith("OBR|")
                        && line.split("|")[4].split("^")[0].trim() !in listOf(
                    OBR_4_1_EPI_ID, OBR_4_1_LEGACY, OBR_4_1_SUBJECT)
            }
        }

        fun getGroupSegmentsForThisOBR(hl7MessageLines: List<String>, obrIdentifier: String) : List<String> {
            val indexOfOBR = hl7MessageLines.indexOfFirst { line ->
                line.startsWith("OBR|")  && line.split("|")[4].split("^")[0].trim() == obrIdentifier
            }
            val nextOBRIndex = hl7MessageLines.withIndex().indexOfFirst { (index, line) ->
                index > indexOfOBR && line.startsWith("OBR|")
            }
            return if (nextOBRIndex == -1) {
                val end = hl7MessageLines.subList(indexOfOBR + 1, hl7MessageLines.size)
                end.filter { it.startsWith("OBX|") || it.startsWith("SPM|")}
            } else {
                hl7MessageLines.subList(indexOfOBR + 1, nextOBRIndex)
            }

        }


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


        /* private */ fun getMmgsFiltered(mmgs: Array<MMG>): Array<MMG> {

            if ( mmgs.size > 1 ) {
                // remove message header block from all but last mmg
                for ( index in 0..mmgs.size - 2) {
                    mmgs[index].blocks = mmgs[index].blocks.filter { block ->
                        block.name != MMG_BLOCK_NAME_MESSAGE_HEADER && block.elements.isNotEmpty()
                    } // .filter
                } // .for
                // remove duplicate blocks that occur in last and next-to-last mmgs
                val lastMMG =  mmgs[mmgs.size - 1]
                val nextToLastMMG = mmgs[mmgs.size - 2]
                // compare blocks of elements in the mmgs
                // if all the elements IDs in one block are all contained within another block,
                // keep the bigger one
                keepBiggerElementSet(lastMMG, nextToLastMMG)
                keepBiggerElementSet(nextToLastMMG, lastMMG)
            } // .if

            return mmgs
        } // .getMmgsFiltered

    private fun keepBiggerElementSet(firstMMG: MMG, secondMMG: MMG) {
        firstMMG.blocks.forEach { block ->
            val blockElementIds = block.elements.map { elem -> elem.mappings.hl7v251.identifier }.toSet()
            secondMMG.blocks = secondMMG.blocks.filter {
                !blockElementIds.containsAll(it.elements.map { el -> el.mappings.hl7v251.identifier }.toSet())
            }
        }
    }
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
                val segmentDataArr = if (segmentDataFull.contains("~")) {
                    if (el.isRepeat || el.mayRepeat.contains("Y"))
                        segmentDataFull.split("~")
                    else
                        listOf(segmentDataFull.split("~")[0])
                } else listOf(segmentDataFull)

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
                                try {
                                    conceptJson = redisClient.hget(REDIS_VOCAB_NAMESPACE + valueSetCode, conceptCode)
                                } catch (e : NullPointerException) {
                                    println("ValueSetCode: $valueSetCode, conceptCode: $conceptCode not found in Redis cache")
                                }
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
                return if (el.isRepeat || el.mayRepeat.contains("Y")) segmentData else segmentData[0]
            } else {
                return null
            }
        } // .getSegmentData

    // } // .companion object
} // .Transformer

