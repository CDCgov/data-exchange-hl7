package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory
import gov.cdc.dex.redisModels.MMG

class Transformer  {

    companion object {
        val logger = LoggerFactory.getLogger(Transformer::class.java.simpleName)
    
        // @Throws(Exception::class)

    fun hl7ToJsonModel(hl7Content: String, mmgs: Array<MMG>) {

        val blocks = mmgs.flatMap { it ->
            it.blocks
        } // .blocks
        
        logger.info("Transformer blocks.size: --> ${blocks.size}")

    } // .hl7ToJsonModel 

    }
} // .Transformer
