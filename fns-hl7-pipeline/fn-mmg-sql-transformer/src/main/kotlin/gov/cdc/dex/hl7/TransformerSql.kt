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
        const val SEPARATOR_ELEMENT_FIELD_NAMES = "~"
    } // .companion object


    // --------------------------------------------------------------------------------------------------------
    //  ------------- MMG Elements that are Single and Non Repeats -------------
    // --------------------------------------------------------------------------------------------------------
    fun singlesNonRepeatsToSqlModel(elements: List<Element>, profilesMap: Map<String, List<PhinDataType>>, modelStr: String) : Map<String, Any?> {

        val modelJson = JsonParser.parseString(modelStr).asJsonObject

        val singlesNonRepeatsModel = elements.flatMap{ el -> 
            val elName = StringUtils.normalizeString(el.name)
            val elDataType = el.mappings.hl7v251.dataType

            val elModel = modelJson[elName]
            
            if (elModel.isJsonNull) {

                // logger.info("${elName} --> ${elModel}")
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

                    sqlPreferred.map{ fld ->
                        
                        val fldNameNorm = StringUtils.normalizeString(fld.name)

                        // logger.info("${elName}~${fldNameNorm} --> ${elObj[fldNameNorm]}")
                        "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$fldNameNorm" to elObj[fldNameNorm]
                    } // .map

                } // .else 
            } // .else

        }.toMap() // .mmgElemsBlocksSingleNonRepeats

        // logger.info("singlesNonRepeatsModel: --> \n\n${gsonWithNullsOn.toJson(singlesNonRepeatsModel)}\n")       
        return singlesNonRepeatsModel
    } // .singlesNonRepeatsToSqlModel


    // --------------------------------------------------------------------------------------------------------
    //  ------------- MMG Elements that are Single and Repeats -------------
    // --------------------------------------------------------------------------------------------------------
    fun singlesRepeatsToSqlModel(elements: List<Element>, profilesMap: Map<String, List<PhinDataType>>, modelStr: String) : Map<String, Any?> {

        val modelJson = JsonParser.parseString(modelStr).asJsonObject

        val singlesRepeatsModel = elements.map{ el -> 

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

                        sqlPreferred.map{ fld ->
                    
                            val fldNameNorm = StringUtils.normalizeString(fld.name)
    
                            // logger.info("${elName}~${fldNameNorm} --> ${elObj[fldNameNorm]}")
                            "$elName$SEPARATOR_ELEMENT_FIELD_NAMES$fldNameNorm" to elObj[fldNameNorm]
                        }.toMap() // .map
                        
                    } // .else 

                } // .elModelArr.map

            } // .else

        }.toMap()

        // logger.info("singlesRepeatsModel: --> \n\n${gsonWithNullsOn.toJson(singlesRepeatsModel)}\n")       
        return singlesRepeatsModel
    } // .singlesRepeatsToSqlModel


    // --------------------------------------------------------------------------------------------------------
    //  ------------- MMG Elements that are Single and Repeats -------------
    // --------------------------------------------------------------------------------------------------------
    fun repeatedBlocksToSqlModel(blocks: List<Block>, profilesMap: Map<String, List<PhinDataType>>, modelStr: String) : Map<String, Any?> {

        val modelJson = JsonParser.parseString(modelStr).asJsonObject

        val repeatedBlocksModel = blocks.map{blk -> 
            val blkName = StringUtils.normalizeString(blk.name)

            val blkModel = modelJson[blkName]

            if ( blkModel.isJsonNull ) {
                blkName to blkModel // this is null
            } else {

                // TODO: sql model for blocks

                val blkModelArr = blkModel.asJsonArray

                blkModelArr.map{ bma -> 
                    val bmaObj = bma.asJsonObject

                    // need element keys for this block
                    val elementsInBlock = blocks.filter{ it.name == blk.name}[0].elements 
                    val elementNames = elementsInBlock.map{ StringUtils.normalizeString(it.name)}

                    logger.info("blkName: --> ${blkName}, elementsNames: ${elementNames}")
                    // TODO: transform each element to sql fields
                
                    
                } // .blkModelArr

                blkName to blkModel
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

