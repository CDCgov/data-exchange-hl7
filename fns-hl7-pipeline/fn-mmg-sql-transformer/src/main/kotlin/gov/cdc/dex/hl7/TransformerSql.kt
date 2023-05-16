package gov.cdc.dex.hl7

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import gov.cdc.dex.hl7.model.PhinDataType
import gov.cdc.dex.redisModels.Block
import gov.cdc.dex.redisModels.Element
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.util.StringUtils
import gov.cdc.dex.util.StringUtils.Companion.normalize
import java.lang.reflect.Type
import java.util.*

class TransformerSql {

    companion object {
        private val gson: Gson = GsonBuilder().serializeNulls().create()
        private const val MMG_BLOCK_NAME_MESSAGE_HEADER = "Message Header"
        const val SEPARATOR = "_"
        private const val ELEMENT_CE = "CE"
        private const val ELEMENT_CWE = "CWE"
        private const val CODE_SYSTEM_CONCEPT_NAME = "code_system_concept_name"
        private const val CDC_PREFERRED_DESIGNATION =  "cdc_preferred_designation"
        private const val MESSAGE_PROFILE_IDENTIFIER_EL_NAME = "message_profile_identifier"
        private const val MESSAGE_PROFILE_ID_ALTERNATE_NAME = "message_profile_id"
        private const val MAX_BLOCK_NAME_LENGTH = 30
        const val MMG_BLOCK_TYPE_SINGLE = "Single"
    } // .companion object


    // --------------------------------------------------------------------------------------------------------
    //  ------------- MMG Elements that are Single and Non Repeats -------------
    // --------------------------------------------------------------------------------------------------------
    fun singlesNonRepeatsToSqlModel(elements: List<Element>, profilesMap: Map<String, List<PhinDataType>>, modelJson: JsonObject) : Map<String, Any?> {

        val singlesNonRepeatsModel = elements.flatMap{ el -> 
            val elName = StringUtils.normalizeString(el.name)
            val elDataType = el.mappings.hl7v251.dataType

            val elModel = modelJson[elName]
            // logger.info("${elName} --> ${elName}, modelJson[elName]: --> ${modelJson[elName]}")

            if (elModel == null || elModel.isJsonNull) {

                listOf(elName to elModel) // elModel is null, passing to model as is

            } else {
                if ( !profilesMap.containsKey(elDataType) ) {

                    val elValue = elModel.asJsonPrimitive
                    // logger.info("${elName} --> ${elValue}")
                    listOf(elName to elValue)

                } else {
                    val mmgDataType = el.mappings.hl7v251.dataType
                    val sqlPreferred = profilesMap[mmgDataType]!!.filter { it.preferred }

                    // logger.info("mmgDataType: $mmgDataType, sqlPreferred.size: --> ${sqlPreferred.size}")
                    val elObj = elModel.asJsonObject

                    val arrFromDefProf = sqlPreferred.map{ fld ->
                        val fldNameNorm = StringUtils.normalizeString(fld.name)
                        // logger.info("${elName}~${fldNameNorm} --> ${elObj[fldNameNorm]}")
                        "$elName$SEPARATOR$fldNameNorm" to elObj[fldNameNorm]
                    } // .map
                    // for the CE and CWE add the code_system_concept_name and cdc_preferred_designation
                    if (mmgDataType == ELEMENT_CE || mmgDataType == ELEMENT_CWE) { 
                        arrFromDefProf + 
                            listOf(
                                "$elName$SEPARATOR$CODE_SYSTEM_CONCEPT_NAME" to elObj[CODE_SYSTEM_CONCEPT_NAME],
                                "$elName$SEPARATOR$CDC_PREFERRED_DESIGNATION" to elObj[CDC_PREFERRED_DESIGNATION]
                                )
                     } else arrFromDefProf

                } // .else 
            } // .else

        }.toMap() // .mmgElemsBlocksSingleNonRepeats

        // logger.info("singlesNonRepeatsModel: --> \n\n${gsonWithNullsOn.toJson(singlesNonRepeatsModel)}\n")       
        return singlesNonRepeatsModel
    } // .singlesNonRepeatsToSqlModel


    // --------------------------------------------------------------------------------------------------------
    //  ------------- MMG Elements that are Single and Repeats -------------
    // --------------------------------------------------------------------------------------------------------
    fun singlesRepeatsToSqlModel(elements: List<Element>, profilesMap: Map<String, List<PhinDataType>>, modelJson: JsonObject) : Map<String, Any?> {

        val singlesRepeatsModel = elements.filter { el ->

            StringUtils.normalizeString(el.name) != MESSAGE_PROFILE_IDENTIFIER_EL_NAME

        }.associate { el ->
            val elName = StringUtils.getNormalizedShortName(el.name, MAX_BLOCK_NAME_LENGTH)
            val elDataType = el.mappings.hl7v251.dataType
            val isPrimitive = !profilesMap.containsKey(elDataType)
            val sqlPreferred = if (!isPrimitive) {
                profilesMap[elDataType]!!.filter { it.preferred }
            } else listOf()
            val elModel = modelJson[elName]
            if (elModel == null || elModel.isJsonNull) {
                elName to elModel // elModel is null, passing to model as is
            } else {
                val elModelArr = elModel.asJsonArray
                elName to elModelArr.map { elMod ->
                    if (!profilesMap.containsKey(elDataType)) {
                        val elValue = elMod.asJsonPrimitive
                        mapOf("$elName${SEPARATOR}value" to elValue)
                    } else {


                        val elObj = elMod.asJsonObject
                        getMapWithPreferredNames(sqlPreferred, elName, elObj, elDataType)
                    } // .else
                } // .elModelArr.map
            } // .else
        }
        // logger.info("singlesRepeatsModel: --> \n\n${gsonWithNullsOn.toJson(singlesRepeatsModel)}\n")
        return singlesRepeatsModel
    } // .singlesRepeatsToSqlModel


    // --------------------------------------------------------------------------------------------------------
    //  ------------- MMG Elements that are Repeated Blocks -------------
    // --------------------------------------------------------------------------------------------------------
    private fun getNormalizedBlockName(block: Block) : String {
        val blockName = if (block.name.normalize().contains("repeating_group")) {
            block.name
        } else {
            "${block.name} repeating group"
        }
        return StringUtils.getNormalizedShortName(blockName, MAX_BLOCK_NAME_LENGTH)
    }

    fun repeatedBlocksToSqlModel(blocks: List<Block>, profilesMap: Map<String, List<PhinDataType>>, modelJson: JsonObject) : Map<String, Any?> {
        val tables = mutableMapOf<String, Any?>()
        blocks.forEach { blk ->
            val blkName = getNormalizedBlockName(blk)
            val blkData = modelJson[blkName]

            if (blkData == null || blkData.isJsonNull || blkData.asJsonArray.isEmpty) {
                tables[blkName] = JsonNull.INSTANCE // we want null, not an empty array
            } else {
                val blkModelArr = blkData.asJsonArray  //array of data for this repeating block
                // need to determine up front if there are any repeating elements within this repeat block
                val elementsInBlock = blocks.filter { it.name == blk.name }[0].elements.associateBy {elem ->
                    elem.mappings.hl7v251.identifier}.values.toList()
                val (repeaters, singles) = elementsInBlock.partition { it.isRepeat || it.mayRepeat.contains("Y") }
                val tableRows = mutableListOf<Map<String, JsonElement>>()
                val subTables = mutableMapOf<String, MutableList<Map<String, JsonElement>>>()
                val idColName = StringUtils.getNormalizedShortName("${blkName}_id")

                blkModelArr.forEach { bma ->
                        val bmaObj = bma.asJsonObject
                        // for each repeated element, create a separate table
                        // a unique ID matches each record with the parent table record
                        val repeatersNames = if (repeaters.isNotEmpty()) {
                            repeaters.map { StringUtils.normalizeString(it.name) }
                        } else {
                            listOf()
                        }
                        // get the singles for the main table
                        val flattenedSingles = if (singles.isNotEmpty()) {
                            val singlesNames = singles.map { StringUtils.normalizeString(it.name) }
                            singlesNames.map { elName ->
                                mapSingleElement(bmaObj, elName, singles, profilesMap)
                            }.reduce { acc, map -> acc + map }
                        } else {
                            mapOf()
                        }
                        // add a column for the unique ID
                        val idColumn = mapOf(idColName to UUID.randomUUID().toString().toJsonElement())
                        tableRows.add(idColumn + flattenedSingles)

                        //create a table for each repeating element
                        //include the unique id to link it back to the parent table record
                        repeatersNames.forEach { elName ->
                            val subTableName = "${blkName}_${StringUtils.getNormalizedShortName(elName)}"
                            // extract each element of the array and create a new Json object
                            // with a key that combines the block name and element name
                            if (!(bmaObj[elName] == null || bmaObj[elName].isJsonNull || bmaObj[elName].asJsonArray.isEmpty)) {
                                val arrayData = bmaObj[elName].asJsonArray
                                val rows = mutableListOf<Map<String, JsonElement>>()
                                for (arrayDatum in arrayData) {
                                    val newObject = JsonObject()
                                    newObject.add(elName, arrayDatum)
                                    rows.add(idColumn + mapSingleElement(newObject, elName, repeaters, profilesMap))
                                }
                                if (subTables[subTableName] != null) {
                                    subTables[subTableName]?.addAll(rows)
                                } else {
                                    subTables[subTableName] = rows
                                }
                            }
                        }
                    } // .forEach array in this block
                    // add the main rg table
                    tables[blkName] = tableRows.toTypedArray().toJsonElement()
                    // add any sub-tables
                    subTables.keys.forEach { key ->
                        tables[key] = subTables[key]?.toTypedArray()?.toJsonElement()
                    }
                }// .else

            }// .forEach block
        return tables
    } // .repeatedBlocksToSqlModel

    fun mapLabTemplate(profilesMap: Map<String, List<PhinDataType>>, modelJson: JsonObject) : Map<String, Any?>? {
        val labTemplate = this::class.java.getResource("/lab_template_v3.json")?.readText(Charsets.UTF_8)
        if (!labTemplate.isNullOrEmpty()) {
            val labMMG = gson.fromJson(labTemplate, MMG::class.java)
            val (labBlocksSingle, labBlocksRepeat) = labMMG.blocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }
            // should be 2 blocks: Lab Template (single) and Tests Performed (repeat)
            // a little deceiving, because the whole thing (Lab Template + Tests Performed sub-table) repeats
            val (labTemplateSingles, labTemplateRepeats) = labBlocksSingle[0].elements.partition { !it.isRepeat && !it.mayRepeat.contains("Y") }
            val labRecords = modelJson["lab_optional_rg"].asJsonArray
            val labRecordsList = mutableListOf<Map<String, Any?>>()
            val labTestResultsList = mutableListOf<Map<String, Any?>>()
            if (!labRecords.isEmpty) {
                labRecords.forEach { record ->
                    val labRecordSingles = singlesNonRepeatsToSqlModel(labTemplateSingles, profilesMap, record.asJsonObject)
                    val labRecordRepeats = singlesRepeatsToSqlModel(labTemplateRepeats, profilesMap, record.asJsonObject)
                    val labRecordRepeatGroup = repeatedBlocksToSqlModel(labBlocksRepeat, profilesMap, record.asJsonObject)
                    // create a unique ID that matches the tests performed to the record
                    val idColumn = mapOf("lab_optional_rg_id" to UUID.randomUUID().toString().toJsonElement())
                    labRecordsList.add(idColumn + labRecordSingles + labRecordRepeats)
                    if (labRecordRepeatGroup.containsKey("tests_performed_rg")) {
                        val testsPerformed = labRecordRepeatGroup["tests_performed_rg"] as JsonArray
                        val mapType: Type = object : TypeToken<Map<String?, Any?>?>() {}.type
                        testsPerformed.forEach { test ->
                            val testMap : Map<String, Any> = gson.fromJson(test, mapType)
                            labTestResultsList.add(idColumn + testMap)
                        }
                    }
                }
                return mapOf("lab_optional_rg" to labRecordsList.toList()) +
                        mapOf("lab_optional_rg_test_results" to labTestResultsList.toList())
            }
        }
        return null
    }

    private fun mapSingleElement(bmaObj: JsonObject, elName: String, elementsInBlock: List<Element>, profilesMap: Map<String, List<PhinDataType>>) : Map<String, JsonElement> {
        val elMod = bmaObj[elName]
        val mmgElement =
            elementsInBlock.filter { StringUtils.normalizeString(it.name) == elName }[0]
        val mmgElDataType = mmgElement.mappings.hl7v251.dataType
        val isPrimitive : Boolean = !profilesMap.containsKey(mmgElDataType)
        val sqlPreferred = if (!isPrimitive) {
            profilesMap[mmgElDataType]!!.filter { it.preferred }
        } else listOf()
        return if (elMod == null || elMod.isJsonNull) {
             if (isPrimitive) {
                mapOf(elName to elMod)
            } else {
                mapPreferredNamesToNull(sqlPreferred, elName, mmgElDataType)
            }
        } else {
            if (isPrimitive) {
                val elValue = elMod.asJsonPrimitive
                mapOf(elName to elValue)
            } else {
                val elObj = elMod.asJsonObject
                getMapWithPreferredNames(sqlPreferred, elName, elObj, mmgElDataType)
            } // .else
        }
    }

    private fun mapPreferredNamesToNull(sqlPreferred: List<PhinDataType>,
                                        elName: String, mmgDataType: String): Map<String, JsonElement> {
        val componentMap = sqlPreferred.associate { component ->
            val compName = StringUtils.normalizeString(component.name)
            "$elName$SEPARATOR$compName" to JsonNull.INSTANCE
        }
        return if (mmgDataType == ELEMENT_CE || mmgDataType == ELEMENT_CWE) {
            componentMap + mapOf(
                "$elName$SEPARATOR$CODE_SYSTEM_CONCEPT_NAME" to JsonNull.INSTANCE,
                "$elName$SEPARATOR$CDC_PREFERRED_DESIGNATION" to JsonNull.INSTANCE
            )
        } else componentMap
    }

    private fun getMapWithPreferredNames(sqlPreferred: List<PhinDataType>,
                                         elName: String, elObj: JsonObject,
                                         mmgDataType: String) : Map<String, JsonElement>  {
        val mapFromDefProf = sqlPreferred.associate { fld ->
            val fldNameNorm = StringUtils.normalizeString(fld.name)
            // logger.info("${elName}~${fldNameNorm} --> ${elObj[fldNameNorm]}")
            "$elName$SEPARATOR$fldNameNorm" to elObj[fldNameNorm]
        } // .map
        // for the CE and CWE add the code_system_concept_name and cdc_preferred_designation
        return if (mmgDataType == ELEMENT_CE || mmgDataType == ELEMENT_CWE) {
            mapFromDefProf +
                    mapOf(
                        "$elName$SEPARATOR$CODE_SYSTEM_CONCEPT_NAME" to elObj[CODE_SYSTEM_CONCEPT_NAME],
                        "$elName$SEPARATOR$CDC_PREFERRED_DESIGNATION" to elObj[CDC_PREFERRED_DESIGNATION]
                    )
        } else mapFromDefProf
    }
    // --------------------------------------------------------------------------------------------------------
    //  ------------- MMG Element: Message Profile Identifier -------------
    // --------------------------------------------------------------------------------------------------------
    fun messageProfIdToSqlModel(modelJson: JsonObject) : Map<String, Any?> {
        val msgProfile = modelJson.get(MESSAGE_PROFILE_IDENTIFIER_EL_NAME) ?: modelJson.get(
            MESSAGE_PROFILE_ID_ALTERNATE_NAME)
        return msgProfile.asJsonArray.mapIndexed{ index, mipPhinObj ->
            MESSAGE_PROFILE_IDENTIFIER_EL_NAME + "_" + (index.toInt()+1).toString() to mipPhinObj.asJsonObject["entity_identifier"]
        }.toMap()

    } // .messageProfIdToSqlModel

    // --------------------------------------------------------------------------------------------------------
    //  ------------- Functions used in the transformation -------------
    // --------------------------------------------------------------------------------------------------------

    fun getMmgsFiltered(mmgs: Array<MMG>): Array<MMG> {
        if ( mmgs.size > 1 ) { 
            for ( index in 0..mmgs.size - 2) { // except the last one
                mmgs[index].blocks = mmgs[index].blocks.filter { block ->
                    block.name != MMG_BLOCK_NAME_MESSAGE_HEADER
                } // .filter
            } // .for
        } // .if

        return mmgs
    } // .getMmgsFiltered

} // .TransformerSql

