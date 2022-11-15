package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory
import gov.cdc.dex.redisModels.MMG

class Transformer  {

    companion object {
        val logger = LoggerFactory.getLogger(Transformer::class.java.simpleName)
        const val MMG_BLOCK_TYPE_SINGLE = "Single"
        private val OBR_4_1_EPI_ID = "68991-9"
    
        //? @Throws(Exception::class) 

        fun hl7ToJsonModel(hl7Content: String, mmgs: Array<MMG>): Map<String, String> {
            // TODO: could there be multiple MSH, PID ? -> TODO: filter out and keep the one's from the condition MMG 

            val messageLines = getMessageLines(hl7Content)

            val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks

            val (mmgBlocksSingle, mmgBlocksNonSingle) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }

            
            // ----------------------------------------------------
            //  ------------- BLOCKS SINGLE
            // ----------------------------------------------------

            val mmgElemsBlocksSingle = mmgBlocksSingle.flatMap { it.elements } // .mmgElemsBlocksSingle
            

            //  ------------- MSH
            // ----------------------------------------------------
            val mmgMsh = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "MSH" }

            val mshMap = mmgMsh.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition - 1
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 

                val mshLineParts = messageLines.filter { it.startsWith("MSH|") }[0].split("|") // there would be only one MSH

                val segmentData = if (mshLineParts.size > dataFieldPosition) mshLineParts[dataFieldPosition] else ""   

                el.name to segmentData 
            }.toMap() // .mmgMsh.map


            //  ------------- PID
            // ----------------------------------------------------
            val mmgPid = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "PID" }

            val pidMap = mmgPid.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 

                val pidLineParts = messageLines.filter { it.startsWith("PID|") }[0].split("|") // there would be only one PID

                val segmentData = if (pidLineParts.size > dataFieldPosition) pidLineParts[dataFieldPosition] else ""  

                el.name to segmentData 
            }.toMap() // .mmgPid.map


            //  ------------- OBR
            // ----------------------------------------------------
            val mmgObr = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "OBR" }

            val obrMap = mmgObr.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 

                val obrLineParts = messageLines.filter { it.startsWith("OBR|") && it.contains(OBR_4_1_EPI_ID) }[0].split("|") // there would be only one Epi Obr
                // logger.info("obrLineParts: $obrLineParts")
                val segmentData = if (obrLineParts.size > dataFieldPosition) obrLineParts[dataFieldPosition] else ""  

                el.name to segmentData 
            }.toMap() // .mmgObr.map

            
            //  ------------- OBX
            // ----------------------------------------------------
            val mmgObx = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "OBX" }

            val obxMap = mmgObx.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 
                val mmgObxIdentifier = el.mappings.hl7v251.identifier

                val obxLines = messageLines.filter { it.startsWith("OBX|") }

                val obxLine = obxLines.filter { line -> 
                    val lineParts = line.split("|")
                    val obxIdentifier = lineParts[3].split("^")[0]
                    mmgObxIdentifier == obxIdentifier
                } // .obxLine
                
                val obxParts = if (obxLine.size > 0) obxLine[0].split("|") else listOf<String>()
                
                val segmentData = if (obxParts.size > dataFieldPosition) obxParts[dataFieldPosition] else ""  

                el.name to segmentData
            }.toMap() // .mmgPid.map


            // ----------------------------------------------------
            //  ------------- BLOCKS NON SINGLE
            // ----------------------------------------------------
            // TODO....
            //val mmgBlocksNonSingle = blocksNonSingle.flatMap { it.elements } // .elemsBlocksSingle

            
            val mmgModelBlocksSingle = mshMap + pidMap + obrMap + obxMap

            logger.info("MMG Model (mmgModelBlocksSingle): --> ${mmgModelBlocksSingle}")
            return mmgModelBlocksSingle
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