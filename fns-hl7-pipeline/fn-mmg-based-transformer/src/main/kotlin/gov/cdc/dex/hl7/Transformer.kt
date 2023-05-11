package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import gov.cdc.dex.TemplateTransformer
import gov.cdc.dex.azure.RedisProxy
import gov.cdc.dex.hl7.model.PhinDataType
import gov.cdc.dex.redisModels.Block
import gov.cdc.dex.redisModels.Element
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.redisModels.ValueSetConcept
import gov.cdc.dex.util.StringUtils
import gov.cdc.dex.util.StringUtils.Companion.normalize
import gov.cdc.hl7.HL7ParseUtils
import gov.cdc.hl7.HL7StaticParser
import java.lang.reflect.Type

class Transformer( redisProxy: RedisProxy, val mmgs: Array<MMG>, val hl7Content: String) {

    private val redisClient = redisProxy.getJedisClient()
    private val hl7Parser: HL7ParseUtils = HL7ParseUtils.getParser(hl7Content, "BasicProfile.json")
    private val phinDataTypesMap = getPhinDataTypes()

    companion object {
        //  val logger: Logger = LoggerFactory.getLogger(Transformer::class.java.simpleName)
        private val gson: Gson = GsonBuilder().serializeNulls().create()

        const val MMG_BLOCK_TYPE_SINGLE = "Single"
        private const val OBR_4_1_EPI_ID = "68991-9"
        private const val OBR_4_1_LEGACY = "NOTF"
        private const val OBR_4_1_SUBJECT = "PERSUBJ"
        private const val MMG_BLOCK_NAME_MESSAGE_HEADER = "Message Header"

        // private val MMG_BLOCK_NAME_SUBJECT_RELATED = "Subject Related"
        private const val REDIS_VOCAB_NAMESPACE = "vocab:"
        private const val ELEMENT_CE = "CE"
        private const val ELEMENT_CWE = "CWE"

        // private val PHIN_DATA_TYPE_KEY_NAME = "phin_data_type" // only used in dev
        private const val CODE_SYSTEM_CONCEPT_NAME_KEY_NAME = "code_system_concept_name"
        private const val CDC_PREFERRED_DESIGNATION_KEY_NAME = "cdc_preferred_designation"
        private const val MAX_BLOCK_NAME_LENGTH = 30
    }



    fun transformMessage():Map<String, Any?> {
        val mmgBlocks = getMmgsFiltered(mmgs).flatMap { it.blocks } // .mmgBlocks
        val (mmgBlocksSingle, mmgBlocksRepeat) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }

        val singleElem = hl7ToJsonModelBlocksSingle(mmgBlocksSingle)
        val repeatElem = hl7ToJsonModelBlocksNonSingle(mmgBlocksRepeat)
        val labElem = hl7ToJsonModelLabTemplate(hl7Content)
        return if (labElem == null) singleElem + repeatElem else singleElem + repeatElem + labElem
    }
    //? @Throws(Exception::class)
    private fun hl7ToJsonModelBlocksSingle(mmgBlocksSingle: List<Block> ): Map<String, Any?> {
        val mmgElemsBlocksSingle = mmgBlocksSingle.flatMap { it.elements } // .mmgElemsBlocksSingle

        return mmgElemsBlocksSingle.associate { el ->
            val hl7Mapping = el.mappings.hl7v251
            val segmentData = when (hl7Mapping.segmentType) {
                "OBX" -> hl7Parser.getValue("${hl7Mapping.segmentType}[@3.1='${hl7Mapping.identifier}']-${hl7Mapping.fieldPosition}")
                "OBR" -> hl7Parser.getValue("OBR[@4.1='$OBR_4_1_EPI_ID||$OBR_4_1_LEGACY']-${hl7Mapping.fieldPosition}")
                else -> hl7Parser.getValue("${hl7Mapping.segmentType}-${hl7Mapping.fieldPosition}")

            }
            val mappedData = if (segmentData.isDefined) {
                mapSegmentData(segmentData.get(), el)
            } else null
            if (el.isRepeat || el.mayRepeat.contains("Y"))
                StringUtils.getNormalizedShortName(el.name, MAX_BLOCK_NAME_LENGTH) to mappedData
            else
                StringUtils.normalizeString(el.name) to mappedData
        }
    } // .hl7ToJsonModelBlocksSingle
    // --------------------------------------------------------------------------------------------------------
    //  ------------- hl7ToJsonModelBlocksNonSingle ------------- BLOCKS NON SINGLE
    // --------------------------------------------------------------------------------------------------------
    //? @Throws(Exception::class)
    private fun hl7ToJsonModelBlocksNonSingle(mmgBlocksRepeat: List<Block>): Map<String, List<Map<String, Any?>>> {
        val blocksNonSingleModel = mmgBlocksRepeat.associate { block ->
            val obxIdToElementMap = block.elements.associateBy { element -> element.mappings.hl7v251.identifier }
            val msgLines = block.elements.flatMap { element ->
                filterByIdentifier(element.mappings.hl7v251.identifier)
            } // .block.elements
            //Get all the OBXs grouped by OBX-4:
            val msgLinesByBlockNumMap = msgLines.map { line ->
                val lineParts = line.split("|")
                lineParts[4] to line
            }.groupBy({ it.first }, { it.second })

            //For each Group, create the Segment Data Map
            val blockElementsNameDataTupMap = msgLinesByBlockNumMap.map { (_, lines) ->
                val mapFromMsg = lines.associate { line ->
                    val obx3 = HL7StaticParser.getFirstValue(line, "OBX-3.1").get()
                    val el = obxIdToElementMap[obx3]!!
                    val obx5 = HL7StaticParser.getValue(line, "OBX-5")
                    val mappedData = if (obx5.isDefined)
                         mapSegmentData(obx5.get(), el)
                    else null
                    StringUtils.normalizeString(el.name) to mappedData
                } // .line

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
        return blocksNonSingleModel
    } // .hl7ToJsonModelBlocksNonSingle

    // -------------------------------------------------------------------------------------------------------
    //---------------------Lab Template--------------------------------
    //--------------------------------------------------------------------------------------------------------
    fun hl7ToJsonModelLabTemplate(hl7Content: String) : Map<String, List<Map<String, Any?>>>? {
        val nonEpiOBRs = getNonEpiOBRs(hl7Content)
        if (nonEpiOBRs.isNotEmpty()) {
            val bumblebee = TemplateTransformer.getTransformerWithResource("/labTemplate.json", "/BasicProfile.json")
            val labsList = mutableListOf<Map<String, Any?>>()
            nonEpiOBRs.forEach { obr ->
                val obr3 = HL7StaticParser.getFirstValue(obr, "OBR[1]-3.1")
                val identifier = obr3.get()
                val obxs = hl7Parser.getValue("OBR[@3.1='$identifier']->OBX")
                val spms = hl7Parser.getValue("OBR[@3.1='$identifier']->SPM")

                val labMessage = StringBuilder()
                labMessage.append("$obr\n")
                if (obxs.isDefined) {
                    obxs.get().iterator().forEach { obxInnerArray ->
                        obxInnerArray.iterator().forEach { obx ->
                            labMessage.append("$obx\n")
                        }
                    }
                }
                if (spms.isDefined) {
                    spms.get().iterator().forEach { spmInnerArray ->
                        spmInnerArray.iterator().forEach { spm ->
                            labMessage.append("${spm}\n")
                        }
                    }
                }

                val labMessageJsonString = bumblebee.transformMessage(labMessage.toString())
                val mapType: Type = object : TypeToken<Map<String?, Any?>?>() {}.type
                val labMap: Map<String, Any> = gson.fromJson(labMessageJsonString, mapType)
                labsList.add(labMap)
            }
            return mapOf("lab_orders_rg" to labsList)
        }
        return null
    }


    // --------------------------------------------------------------------------------------------------------
    //  ------------- Functions used in the transformation -------------
    // --------------------------------------------------------------------------------------------------------
    private fun getNonEpiOBRs(hl7Content: String) : List<String> {
        val hl7MessageLines = hl7Content.split("\n")
        return hl7MessageLines.filter { line ->
            line.startsWith("OBR|")
                    && line.split("|")[4].split("^")[0].trim() !in listOf(
                OBR_4_1_EPI_ID, OBR_4_1_LEGACY, OBR_4_1_SUBJECT)
        }
    }

    private fun filterByIdentifier(lines: List<String>, id: String): List<String> {
        val group = lines.joinToString("\n")
        val mappinglines = HL7StaticParser.getValue(group, "OBX[@3.1='$id']")
        return if (mappinglines.isDefined) {
            mappinglines.get().flatten()
        } else listOf()
    }
    private fun filterByIdentifier(id: String): List<String> {
        val mappinglines = hl7Parser.getValue("OBX[@3.1='$id']")
        return if (mappinglines.isDefined) {
                 mappinglines.get().flatten()
            } else listOf()
    }

    fun getMmgsFiltered(mmgs: Array<MMG>): Array<MMG> {
        if (mmgs.size > 1) {
            // remove message header block from all but last mmg
            for (index in 0..mmgs.size - 2) {
                mmgs[index].blocks = mmgs[index].blocks.filter { block ->
                    block.name != MMG_BLOCK_NAME_MESSAGE_HEADER && block.elements.isNotEmpty()
                } // .filter
            } // .for
            // remove duplicate blocks that occur in last and next-to-last mmgs
            val lastMMG = mmgs[mmgs.size - 1]
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

    private fun getPhinDataTypes(): Map<String, List<PhinDataType>> {
        val dataTypesFilePath = "/DefaultFieldsProfileX.json"
        val dataTypesMapJson = this::class.java.getResource(dataTypesFilePath).readText()
        val dataTypesMapType = object : TypeToken<Map<String, List<PhinDataType>>>() {}.type
        return gson.fromJson(dataTypesMapJson, dataTypesMapType)
    } // .getPhinDataTypes


    private fun mapSegmentData(data: Array<Array<String>>?, el: Element): Any? {
        data?.let {
            var fullData = data.flatten()   //flatten data:
            if (!el.isRepeat && !(el.mayRepeat.contains("Y"))) { //If element is not supposed to repeat, get First entry
                fullData = listOf(fullData[0])
            }
            val segmentData = fullData.map { oneRepeat ->
                val oneRepeatParts = oneRepeat.split("^")
                if (phinDataTypesMap.contains(el.mappings.hl7v251.dataType)) {
                    val map1 = phinDataTypesMap[el.mappings.hl7v251.dataType]!!.associate { phinDataTypeEntry ->
                        val fieldNumber = phinDataTypeEntry.fieldNumber - 1
                        val dt = if (oneRepeatParts.size > fieldNumber && oneRepeatParts[fieldNumber].isNotEmpty())
                                oneRepeatParts[fieldNumber]
                            else null
                        if (dt != null) {
                            if (dt.contains("&")) { //Subcomponents are present = need to split them...
                                val subRepeatParts = dt.split("&")

                                val subMap = phinDataTypesMap[phinDataTypeEntry.dataType]!!.associate { subEntries ->
                                    val subFieldNbr = subEntries.fieldNumber -1
                                    val subDt = if (subRepeatParts.size > subFieldNbr && subRepeatParts[subFieldNbr].isNotEmpty())
                                                    subRepeatParts[subFieldNbr]
                                                else null
                                    StringUtils.normalizeString(subEntries.name)to subDt
                                }
                                StringUtils.normalizeString(phinDataTypeEntry.name) to subMap
                            } else {
                                StringUtils.normalizeString(phinDataTypeEntry.name) to dt
                            }
                        } else
                          StringUtils.normalizeString(phinDataTypeEntry.name) to null
                    }
                    // call vocab for preferred name and cdc preferred name
                    if (el.mappings.hl7v251.dataType in arrayOf(ELEMENT_CE, ELEMENT_CWE)) {
                       map1 + getPhinVadsConcepts(el.valueSetCode, map1["identifier"].toString())
                    } else {
                        // this is not an ELEMENT_CE || ELEMENT_CWE
                        map1 //+ map2
                    } // .else
                } else {
                    // not available in the default fields profile (DefaultFieldsProfile.json)
                    // considering the component position
                    // dtCompPos data component position
                    when (val dtCompPos = el.mappings.hl7v251.componentPosition - 1) {
                        in 0..Int.MAX_VALUE -> if (oneRepeatParts.size > dtCompPos && oneRepeatParts[dtCompPos].isNotEmpty()) oneRepeatParts[dtCompPos] else null
                        // no need to use component position
                        else -> oneRepeat // full data (string)
                    } // .when

                } // .else

            }
           return if (el.isRepeat || el.mayRepeat.contains("Y")) segmentData else segmentData[0]
        }
        return null
    }

    private fun getPhinVadsConcepts(valueSetCode: String?, conceptCode: String?): Map<String, String?> {
        var conceptJson = ""
        if ((!valueSetCode.isNullOrEmpty() && valueSetCode != "N/A") && !conceptCode.isNullOrEmpty()) {
            try {
                conceptJson = redisClient.hget(REDIS_VOCAB_NAMESPACE + valueSetCode, conceptCode)
            } catch (e : NullPointerException) {
                println("ValueSetCode: $valueSetCode, conceptCode: $conceptCode not found in Redis cache")
            }
        }
        return if (conceptJson.isEmpty())
        { // map2 used for dev only
            // No Redis entry!! for this value set code, concept code
            mapOf(
                CODE_SYSTEM_CONCEPT_NAME_KEY_NAME to null,
                CDC_PREFERRED_DESIGNATION_KEY_NAME to null
            )
        } else {
            // logger.info("ValueSetConcept conceptJson: --> $conceptJson")
            val cobj: ValueSetConcept = gson.fromJson(conceptJson, ValueSetConcept::class.java)
            mapOf(
                CODE_SYSTEM_CONCEPT_NAME_KEY_NAME to cobj.codeSystemConceptName,
                CDC_PREFERRED_DESIGNATION_KEY_NAME to cobj.cdcPreferredDesignation
            )
        }
}

    // } // .companion object
} // .Transformer

