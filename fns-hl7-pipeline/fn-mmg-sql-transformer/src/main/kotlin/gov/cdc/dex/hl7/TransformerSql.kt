package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory
import gov.cdc.dex.redisModels.MMG
// import gov.cdc.dex.redisModels.Block
// import gov.cdc.dex.redisModels.Element
// import gov.cdc.dex.redisModels.ValueSetConcept

import gov.cdc.dex.hl7.model.PhinDataType

import gov.cdc.dex.util.StringUtils

// import com.google.gson.Gson 
// import com.google.gson.GsonBuilder
// import com.google.gson.reflect.TypeToken

import com.google.gson.JsonObject
import com.google.gson.JsonParser

import  gov.cdc.dex.azure.RedisProxy 

class TransformerSql()  {

    companion object {
        val logger = LoggerFactory.getLogger(TransformerSql::class.java.simpleName)
        // private val gson = Gson()
        // private val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()
        private val MMG_BLOCK_NAME_MESSAGE_HEADER = "Message Header" 
        const val MMG_BLOCK_TYPE_SINGLE = "Single"
    } // .companion object

    fun toSqlModel(mmgsArr: Array<MMG>, profilesMap: Map<String, List<PhinDataType>>, modelStr: String) : Int {

        val mmgs = getMmgsFiltered(mmgsArr)
        val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks
        val (mmgBlocksSingle, _) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }
        val mmgElemsBlocksSingleNonRepeats = mmgBlocksSingle.flatMap { it.elements }.filter{ !it.isRepeat }

        val modelJson = JsonParser.parseString(modelStr).asJsonObject

        mmgElemsBlocksSingleNonRepeats.forEach{ el -> 
            val elName = StringUtils.normalizeString(el.name)
            val elDataType = el.mappings.hl7v251.dataType

            val elModel = modelJson[elName]
            

            if (elModel.isJsonNull) {
                logger.info("elName: --> ${elName}, elInModel: --> ${elModel}")
            } else {
                if ( !profilesMap.containsKey(elDataType) ) {

                    val elValue = elModel.asJsonPrimitive
                    logger.info("elName: --> ${elName}, elValue: --> ${elValue}")

                } else {
                    val elObj = elModel.asJsonObject
                    logger.info("elName: --> ${elName}, elObj: --> ${elObj}")

                } // .else 
            } // .else

        
        } // .mmgElemsBlocksSingleNonRepeats

        
        return 42
    } // .toSqlModel


    // --------------------------------------------------------------------------------------------------------
    //  ------------- Functions used in the transformation -------------
    // --------------------------------------------------------------------------------------------------------

    private fun getMmgsFiltered(mmgs: Array<MMG>): Array<MMG> {

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

