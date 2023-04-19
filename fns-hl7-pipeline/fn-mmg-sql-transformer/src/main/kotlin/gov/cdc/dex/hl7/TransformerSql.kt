package gov.cdc.dex.hl7

//import org.slf4j.LoggerFactory
// import gov.cdc.dex.redisModels.ValueSetConcept

// import com.google.gson.Gson
// import com.google.gson.reflect.TypeToken

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import gov.cdc.dex.hl7.model.PhinDataType
import gov.cdc.dex.redisModels.Block
import gov.cdc.dex.redisModels.Element
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.util.StringUtils
import gov.cdc.dex.util.StringUtils.Companion.normalize

class TransformerSql {

    companion object {
        private const val MMG_BLOCK_NAME_MESSAGE_HEADER = "Message Header"
        const val SEPARATOR = "_"
        private const val ELEMENT_CE = "CE"
        private const val ELEMENT_CWE = "CWE"
        private const val CODE_SYSTEM_CONCEPT_NAME = "code_system_concept_name"
        private const val CDC_PREFERRED_DESIGNATION =  "cdc_preferred_designation"
        private const val MESSAGE_PROFILE_IDENTIFIER_EL_NAME = "message_profile_identifier"
        private const val MESSAGE_PROFILE_ID_ALTERNATE_NAME = "message_profile_id"
        private const val MAX_BLOCK_NAME_LENGTH = 30
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
                        val mmgDataType = el.mappings.hl7v251.dataType
                        val sqlPreferred = profilesMap[mmgDataType]!!.filter { it.preferred }
                        val elObj = elMod.asJsonObject
                        getMapWithPreferredNames(sqlPreferred, elName, elObj, mmgDataType)
                    } // .else
                } // .elModelArr.map
            } // .else
        }
        // logger.info("singlesRepeatsModel: --> \n\n${gsonWithNullsOn.toJson(singlesRepeatsModel)}\n")
        return singlesRepeatsModel
    } // .singlesRepeatsToSqlModel

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
    //  ------------- MMG Elements that are Repeated Blocks -------------
    // --------------------------------------------------------------------------------------------------------
    fun repeatedBlocksToSqlModel(blocks: List<Block>, profilesMap: Map<String, List<PhinDataType>>, modelJson: JsonObject) : Map<String, Any?> {
        var listOfLists = false
        val repeatedBlocksModel = blocks.associate { blk ->
            val blockName = if (blk.name.normalize().contains("repeating_group")) {
                blk.name
            } else {
                "${blk.name} repeating group"
            }
            val blkName = StringUtils.getNormalizedShortName(blockName, MAX_BLOCK_NAME_LENGTH)
            val blkModel = modelJson[blkName]

            if (blkModel == null || blkModel.isJsonNull || blkModel.asJsonArray.isEmpty) {
                blkName to null// we want null, not an empty array
            } else {
                val blkModelArr = blkModel.asJsonArray  //array of data for this repeating block
                // need to determine up front if there are any repeating elements within this repeat block
                val elementsInBlock = blocks.filter { it.name == blk.name }[0].elements
                val elementNames = elementsInBlock.map { StringUtils.normalizeString(it.name) }
                val (repeaters, singles) = elementsInBlock.partition { it.isRepeat || it.mayRepeat.contains("Y") }

                val mOut =
                    blkModelArr.map { bma ->
                        val bmaObj = bma.asJsonObject
                        // logger.info("blkName: --> ${blkName}, elementsNames: ${elementNames}")
                        if (repeaters.isEmpty()) {
                            // we only have single elements to map
                            elementNames.map { elName ->
                                mapSingleElement(bmaObj, elName, elementsInBlock, profilesMap)
                            }.reduce { acc, next -> acc + next }// .elementNames.map
                        } else {
                            listOfLists = true // we will need to fix this at the end
                            // for each repeated element, we need a separate row in the table that also
                            // duplicates the non-repeating elements
                            val repeatersNames = repeaters.map { StringUtils.normalizeString(it.name) }
                             // get the singles that we will need to repeat for each row
                            val singlesNames = singles.map { StringUtils.normalizeString(it.name) }
                            val flattenedSingles = singlesNames.map { elName ->
                                mapSingleElement(bmaObj, elName, singles, profilesMap)
                            }.reduce {acc, map -> acc + map}
                            val rows = mutableListOf<Map<String, JsonElement>>()
                            repeatersNames.forEach { elName ->
                                // extract each element of the array and create a new Json object
                                // with the same key as the key to the array
                                if (bmaObj[elName] == null || bmaObj[elName].isJsonNull || bmaObj[elName].asJsonArray.isEmpty) {
                                    rows.add(flattenedSingles + mapOf(elName to (bmaObj[elName] as JsonNull)))
                                } else {
                                    val arrayData = bmaObj[elName].asJsonArray
                                    for (arrayDatum in arrayData) {
                                        val newObject = JsonObject()
                                        newObject.add(elName, arrayDatum)
                                        val flattenedRepeat =
                                            mapSingleElement(newObject, elName, repeaters, profilesMap)
                                        rows.add(flattenedSingles + flattenedRepeat)
                                    }
                                }
                            }
                            rows.toList()
                        }
                    } //blkModelArr.map
                if (listOfLists) {
                    // unwrap this list of lists into a single list
                    listOfLists = false
                    val newList = mutableListOf<Any>()
                    mOut.forEach{
                        listInIt ->
                        if (listInIt is Iterable<*>) newList.addAll(listInIt as Collection<Any>) //IntelliJ will highlight this but it's OK
                    }
                    blkName to newList
                } else {
                    blkName to mOut
                }

            }// .else

        } // .repeatedBlocksModel
        // logger.info("repeatedBlocksModel: --> \n\n${gsonWithNullsOn.toJson(repeatedBlocksModel)}\n")
        return repeatedBlocksModel
    } // .repeatedBlocksToSqlModel
    private fun mapSingleElement(bmaObj: JsonObject, elName: String, elementsInBlock: List<Element>, profilesMap: Map<String, List<PhinDataType>>) : Map<String, JsonElement> {
        val elMod = bmaObj[elName]
        //logger.info("blkName: --> ${blkName}, elName: $elName, bmaObj: ${bmaObj}")
        return if (elMod == null || elMod.isJsonNull) {
            mapOf(elName to elMod)
        } else {
            val mmgElement =
                elementsInBlock.filter { StringUtils.normalizeString(it.name) == elName }[0]
            val mmgElDataType = mmgElement.mappings.hl7v251.dataType
            if (!profilesMap.containsKey(mmgElDataType)) {
                val elValue = elMod.asJsonPrimitive
                mapOf(elName to elValue)
            } else {
                val sqlPreferred = profilesMap[mmgElDataType]!!.filter { it.preferred }
                val elObj = elMod.asJsonObject
                getMapWithPreferredNames(sqlPreferred, elName, elObj, mmgElDataType)
            } // .else
        }
    }

    // --------------------------------------------------------------------------------------------------------
    //  ------------- MMG Element: Message Profile Identifier -------------
    // --------------------------------------------------------------------------------------------------------
    fun messageProfIdToSqlModel(modelJson: JsonObject) : Map<String, Any?> {
        val msgProfile = modelJson.get(MESSAGE_PROFILE_IDENTIFIER_EL_NAME) ?: modelJson.get(
            MESSAGE_PROFILE_ID_ALTERNATE_NAME)
        return msgProfile.asJsonArray.mapIndexed{ index, mipPhinObj ->
            MESSAGE_PROFILE_IDENTIFIER_EL_NAME + "_" + index.toString() to mipPhinObj.asJsonObject["entity_identifier"]
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

