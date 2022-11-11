package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory
import gov.cdc.dex.redisModels.MMG

class Transformer  {

    companion object {
        val logger = LoggerFactory.getLogger(Transformer::class.java.simpleName)
        const val MMG_BLOCK_TYPE_SINGLE = "Single"
    
        //? @Throws(Exception::class) 

        fun hl7ToJsonModel(hl7Content: String, mmgs: Array<MMG>): Map<String, String> {
            // TODO: could there be multiple MSH, PID ? -> TODO: filter out and keep the one's from the condition MMG 

            val messageLines = getMessageLines(hl7Content)

            val blocks = mmgs.flatMap { it.blocks } // .blocks

            val (blocksSingle, blocksNonSingle) = blocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }

            val elemsBlocksSingle = blocksSingle.flatMap { it.elements } // .elemsBlocksSingle
            

            //  ------------- MSH
            // ----------------------------------------------------
            val mmgMsh = elemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "MSH" }

            val mshMap = mmgMsh.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition -1 
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 

                val mshLineParts = messageLines.filter { it.startsWith("MSH|") }[0].split("|") // there would be only one MSH

                val segmentData = if (mshLineParts.size - 1 >= dataFieldPosition) mshLineParts[dataFieldPosition] else ""   

                el.name to segmentData 
            }.toMap() // .mmgMsh.map


            //  ------------- PID
            // ----------------------------------------------------
            val mmgPid = elemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "PID" }
            val pidMap = mmgPid.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition - 1 // for array position
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 

                val pidLineParts = messageLines.filter { it.startsWith("PID|") }[0].split("|") // there would be only one PID

                val segmentData = if (pidLineParts.size -1 >= dataFieldPosition) pidLineParts[dataFieldPosition] else ""   

                el.name to segmentData 
            }.toMap() // .mmgPid.map

            
            val model = mshMap + pidMap 

            logger.info("model: --> ${model}")
            return model
        } // .hl7ToJsonModel 

        private fun getMessageLines(hl7Content: String): List<String> {
            return hl7Content.split("\n")
        } // .getMessageLines

    } // .companion object
} // .Transformer



            /*
            Element(ordinal=1, name=Message Profile Identifier, dataType=Text, isUnitOfMeasure=false, priority=R, 
            isRepeat=true, repetitions=2, mayRepeat=Y/2, valueSetCode=, valueSetVersionNumber=null, codeSystem=N/A, 
            mappings=Mapping(hl7v251=HL7Mapping(
            legacyIdentifier=NOT115, identifier=N/A: MSH-21, dataType=EI, segmentType=MSH, obrPosition=1, fieldPosition=21, componentPosition=-1, 
            usage=R, cardinality=[2..2], repeatingGroupElementType=NO)))
            */