package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.redisModels.Block
import gov.cdc.dex.redisModels.Element
// import gov.cdc.dex.redisModels.ValueSetConcept

import gov.cdc.dex.hl7.model.PhinDataType

import gov.cdc.dex.util.StringUtils

// import com.google.gson.Gson 
import com.google.gson.GsonBuilder
// import com.google.gson.reflect.TypeToken

import com.google.gson.JsonObject
import com.google.gson.JsonParser

import  gov.cdc.dex.azure.RedisProxy 

class TransformerSql()  {

    companion object {
        val logger = LoggerFactory.getLogger(TransformerSql::class.java.simpleName)
        // private val gson = Gson()
        private val gsonWithNullsOn = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()
        private val MMG_BLOCK_NAME_MESSAGE_HEADER = "Message Header" 
        const val SEPARATOR_ELEMENT_FIELD_NAMES = "_"
        //
        private val ELEMENT_CE = "CE"
        private val ELEMENT_CWE = "CWE"
        private val CODE_SYSTEM_CONCEPT_NAME_KEY_NAME = "code_system_concept_name"
        private val CDC_PREFERRED_DESIGNATION_KEY_NAME =  "cdc_preferred_designation"

        private val MESSAGE_PROFILE_IDENTIFIER_EL_NAME = "message_profile_identifier"
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

            if (elModel.isJsonNull) {

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
                        "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$fldNameNorm" to elObj[fldNameNorm]
                    } // .map
                    // for the CE and CWE add the code_system_concept_name and cdc_preferred_designation
                    if (mmgDataType == ELEMENT_CE || mmgDataType == ELEMENT_CWE) { 
                        arrFromDefProf + 
                            listOf(
                                "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$CODE_SYSTEM_CONCEPT_NAME_KEY_NAME" to elObj[CODE_SYSTEM_CONCEPT_NAME_KEY_NAME], 
                                "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$CDC_PREFERRED_DESIGNATION_KEY_NAME" to elObj[CDC_PREFERRED_DESIGNATION_KEY_NAME]
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

        val singlesRepeatsModel = elements.filter{ el -> 

            StringUtils.normalizeString(el.name) != MESSAGE_PROFILE_IDENTIFIER_EL_NAME
            
        }.map{ el -> 

            val elName = StringUtils.normalizeString(el.name)
            val elDataType = el.mappings.hl7v251.dataType

            val elModel = modelJson[elName]

            if (elModel.isJsonNull) {

                elName to elModel // elModel is null, passing to model as is

            } else {

                val elModelArr = elModel.asJsonArray 

                elName to elModelArr.map{ elMod -> 

                    if ( !profilesMap.containsKey(elDataType) ) {

                        val elValue = elMod.asJsonPrimitive
                        // logger.info("${elName} --> ${elValue}")

                        elName to mapOf(elName to elValue)
    
                    } else {
                        val mmgDataType = el.mappings.hl7v251.dataType
                        val sqlPreferred = profilesMap[mmgDataType]!!.filter { it.preferred }
    
                        val elObj = elMod.asJsonObject

                        val mapFromDefProf = sqlPreferred.map{ fld ->
                    
                            val fldNameNorm = StringUtils.normalizeString(fld.name)
    
                            // logger.info("${elName}~${fldNameNorm} --> ${elObj[fldNameNorm]}")
                            "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$fldNameNorm" to elObj[fldNameNorm]
                        }.toMap() // .map
                        // for the CE and CWE add the code_system_concept_name and cdc_preferred_designation
                        if (mmgDataType == ELEMENT_CE || mmgDataType == ELEMENT_CWE) { 
                            mapFromDefProf + 
                                mapOf(
                                    "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$CODE_SYSTEM_CONCEPT_NAME_KEY_NAME" to elObj[CODE_SYSTEM_CONCEPT_NAME_KEY_NAME], 
                                    "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$CDC_PREFERRED_DESIGNATION_KEY_NAME" to elObj[CDC_PREFERRED_DESIGNATION_KEY_NAME]
                                    )
                        } else mapFromDefProf
                        
                    } // .else 

                } // .elModelArr.map

            } // .else

        }.toMap()

        // logger.info("singlesRepeatsModel: --> \n\n${gsonWithNullsOn.toJson(singlesRepeatsModel)}\n")       
        return singlesRepeatsModel
    } // .singlesRepeatsToSqlModel


    // --------------------------------------------------------------------------------------------------------
    //  ------------- MMG Elements that are Repeated Blocks -------------
    // --------------------------------------------------------------------------------------------------------
    fun repeatedBlocksToSqlModel(blocks: List<Block>, profilesMap: Map<String, List<PhinDataType>>, modelJson: JsonObject) : Map<String, Any?> {

        val repeatedBlocksModel = blocks.map{blk -> 
            val blkName = StringUtils.normalizeString(blk.name)

            val blkModel = modelJson[blkName]

            if ( blkModel.isJsonNull ) {
                blkName to blkModel // this is null
            } else {

                // TODO: sql model for blocks

                val blkModelArr = blkModel.asJsonArray

                val mOut = blkModelArr.map{ bma -> 
                    val bmaObj = bma.asJsonObject

                    // need element keys for this block
                    val elementsInBlock = blocks.filter{ it.name == blk.name}[0].elements 
                    val elementNames = elementsInBlock.map{ StringUtils.normalizeString(it.name)}

                    // logger.info("blkName: --> ${blkName}, elementsNames: ${elementNames}")

                    elementNames.map{ elName -> 
                        val elMod = bmaObj[elName] 
                        //logger.info("blkName: --> ${blkName}, elName: $elName, bmaObj: ${bmaObj}")

                        if ( elMod.isJsonNull ) {

                            mapOf(elName to elMod)
                        } else {

                            val mmgElement = elementsInBlock.filter{ StringUtils.normalizeString(it.name) == elName }[0]

                            val mmgElDataType = mmgElement.mappings.hl7v251.dataType
    
                            if ( !profilesMap.containsKey(mmgElDataType) ) {
    
                                val elValue = elMod.asJsonPrimitive
        
                                mapOf(elName to elValue)
                            } else {
    
                                val sqlPreferred = profilesMap[mmgElDataType]!!.filter { it.preferred }
            
                                val elObj = elMod.asJsonObject
        
                                val mapFromDefProf = sqlPreferred.map{ fld ->
                                    val fldNameNorm = StringUtils.normalizeString(fld.name)
                                    // logger.info("${elName}~${fldNameNorm} --> ${elObj[fldNameNorm]}")
                                    "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$fldNameNorm" to elObj[fldNameNorm]
                                }.toMap() // .map
                                // for the CE and CWE add the code_system_concept_name and cdc_preferred_designation
                                if (mmgElDataType == ELEMENT_CE || mmgElDataType == ELEMENT_CWE) { 
                                    mapFromDefProf + 
                                        mapOf(
                                            "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$CODE_SYSTEM_CONCEPT_NAME_KEY_NAME" to elObj[CODE_SYSTEM_CONCEPT_NAME_KEY_NAME], 
                                            "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$CDC_PREFERRED_DESIGNATION_KEY_NAME" to elObj[CDC_PREFERRED_DESIGNATION_KEY_NAME]
                                            )
                                } else mapFromDefProf

                            } // .else 

                        } // .else 

                    }.reduce { acc, next -> acc + next }// .elementNames.map
                    
                } // .blkModelArr

                blkName to mOut
            } // .else 
        
        }.toMap() // .repeatedBlocksModel

        // logger.info("repeatedBlocksModel: --> \n\n${gsonWithNullsOn.toJson(repeatedBlocksModel)}\n")       
        return repeatedBlocksModel
    } // .repeatedBlocksToSqlModel


    // --------------------------------------------------------------------------------------------------------
    //  ------------- Functions used in the transformation -------------
    // --------------------------------------------------------------------------------------------------------

    fun getMmgsFiltered(mmgs: Array<MMG>): Array<MMG> {
        if ( mmgs.size > 1 ) { 
            for ( index in 0..mmgs.size - 2) { // except the last one
                mmgs[index].blocks = mmgs[index].blocks.filter { block ->
                    !( block.name == MMG_BLOCK_NAME_MESSAGE_HEADER ) //|| block.name == MMG_BLOCK_NAME_SUBJECT_RELATED )
                } // .filter
            } // .for
        } // .if

        return mmgs
    } // .getMmgsFiltered

} // .TransformerSql

