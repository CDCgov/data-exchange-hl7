package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory
import gov.cdc.dex.redisModels.MMG
import gov.cdc.dex.redisModels.Block
// import gov.cdc.dex.redisModels.Element
import gov.cdc.dex.util.StringUtils

class Transformer  {

    companion object {
        val logger = LoggerFactory.getLogger(Transformer::class.java.simpleName)
        const val MMG_BLOCK_TYPE_SINGLE = "Single"
        private val OBR_4_1_EPI_ID = "68991-9"
    

        // --------------------------------------------------------------------------------------------------------
        //  ------------- hl7ToJsonModelBlocksSingle ------------- BLOCKS SINGLE
        // --------------------------------------------------------------------------------------------------------
        //? @Throws(Exception::class) 
        
        fun hl7ToJsonModelBlocksSingle(hl7Content: String, mmgs: Array<MMG>): Map<String, String> {

            // there could be multiple MMGs each with MSH, PID -> filter out and only keep the one's from the last MMG 
            // TODO test 
            if ( mmgs.size > 1 ) { 
                for ( index in 0..mmgs.size - 2) { // except the last one
                    mmgs[index].blocks = mmgs[index].blocks.filter { block ->
                        block.type == MMG_BLOCK_TYPE_SINGLE && block.elements.any { it ->
                            it.mappings.hl7v251.segmentType != "MSH" || it.mappings.hl7v251.segmentType != "PID"
                        } // .it
                    } // .filter
                } // .for
            } // .if
 

            val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks

            val (mmgBlocksSingle, _) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }

            val messageLines = getMessageLines(hl7Content)
            
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

                StringUtils.normalizeString(el.name) to segmentData 
            }.toMap() // .mmgMsh.map


            //  ------------- PID
            // ----------------------------------------------------
            val mmgPid = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "PID" }

            val pidMap = mmgPid.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 

                val pidLineParts = messageLines.filter { it.startsWith("PID|") }[0].split("|") // there would be only one PID

                val segmentData = if (pidLineParts.size > dataFieldPosition) pidLineParts[dataFieldPosition] else ""  

                StringUtils.normalizeString(el.name) to segmentData 
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

                StringUtils.normalizeString(el.name) to segmentData 
            }.toMap() // .mmgObr.map

            
            //  ------------- OBX
            // ----------------------------------------------------
            val mmgObx = mmgElemsBlocksSingle.filter { it.mappings.hl7v251.segmentType == "OBX" }

            val obxLines = messageLines.filter { it.startsWith("OBX|") }

            val obxMap = mmgObx.map { el -> 

                val dataFieldPosition = el.mappings.hl7v251.fieldPosition
                // val dataComponentPosition = el.mappings.hl7v251.componentPosition 
                val mmgObxIdentifier = el.mappings.hl7v251.identifier

                val obxLine = obxLines.filter { line -> 
                    val lineParts = line.split("|")
                    val obxIdentifier = lineParts[3].split("^")[0]
                    mmgObxIdentifier == obxIdentifier
                } // .obxLine
                
                val obxParts = if (obxLine.size > 0) obxLine[0].split("|") else listOf<String>()
                
                val segmentData = if (obxParts.size > dataFieldPosition) obxParts[dataFieldPosition] else ""  

                StringUtils.normalizeString(el.name) to segmentData
            }.toMap() // .mmgPid.map

            
            val mmgModelBlocksSingle = mshMap + pidMap + obrMap + obxMap

            logger.info("MMG Model (mmgModelBlocksSingle): --> ${mmgModelBlocksSingle}")
            return mmgModelBlocksSingle
        } // .hl7ToJsonModelBlocksSingle 
        

        // --------------------------------------------------------------------------------------------------------
        //  ------------- hl7ToJsonModelBlocksNonSingle ------------- BLOCKS SINGLE
        // --------------------------------------------------------------------------------------------------------
        
        fun hl7ToJsonModelBlocksNonSingle(hl7Content: String, mmgs: Array<MMG>): Map<String, List<Pair<String, String>>> {

            // there could be multiple MMGs each with MSH, PID -> filter out and only keep the one's from the last MMG 
            // TODO test 
            if ( mmgs.size > 1 ) { 
                for ( index in 0..mmgs.size - 2) { // except the last one
                    mmgs[index].blocks = mmgs[index].blocks.filter { block ->
                        block.type == MMG_BLOCK_TYPE_SINGLE && block.elements.any { it ->
                            it.mappings.hl7v251.segmentType != "MSH" || it.mappings.hl7v251.segmentType != "PID"
                        } // .it
                    } // .filter
                } // .for
            } // .if
 

            val mmgBlocks = mmgs.flatMap { it.blocks } // .mmgBlocks

            val (_, mmgBlocksNonSingle) = mmgBlocks.partition { it.type == MMG_BLOCK_TYPE_SINGLE }

            val messageLines = getMessageLines(hl7Content)

            val obxLines = messageLines.filter { it.startsWith("OBX|") }

            // ----------------------------------------------------
            //  ------------- BLOCKS NON SINGLE
            // ----------------------------------------------------

            val obxIdToElNameMap = getObxIdToElNameMap(mmgBlocksNonSingle)

            val blocksNonSingleModel = mmgBlocksNonSingle.map{ block -> 

                val msgLines = block.elements.flatMap { element -> 

                    // logger.info("element: --> ${element.mappings.hl7v251.identifier}\n")
                    val mmgObxIdentifier = element.mappings.hl7v251.identifier

                    obxLines.filter { line -> 
                        val lineParts = line.split("|")
                        val obxIdentifier = lineParts[3].split("^")[0]
                        mmgObxIdentifier == obxIdentifier
                    } // .obxLine
                    // logger.info("elemLines: --> ${elemLines}\n")

                    // return elemLines
                } // .block.elements

                val msgLinesByBlockNumMap = msgLines.map { line -> 
                    val lineParts = line.split("|")
                    val blockNum = lineParts[4]
                    blockNum to line
                }.groupBy( { it.first }, { it.second } )


                val elemsDataTup = msgLinesByBlockNumMap.flatMap { (_, lines) -> 
                    lines.map { line -> 
                        val lineParts = line.split("|")
                        val dataFieldPosition = 5 //element.mappings.hl7v251.fieldPosition
                        val segmentData = if (lineParts.size > dataFieldPosition) lineParts[dataFieldPosition] else "" 
                        val obxIdentifier = lineParts[3].split("^")[0]

                        val elName = obxIdToElNameMap.getOrElse(obxIdentifier) { "TODO:throw_error?" }

                        StringUtils.normalizeString(elName) to segmentData
                    } // .lines
                } // .elemsDataTup

                StringUtils.normalizeString(block.name) to elemsDataTup
            }.toMap() // .blocksNonSingleModel

            
            // for ((key, value) in blockToLinesMap) {
            //     logger.info("value --> ${value}")
            // }
            logger.info("blocksNonSingleModel: --> ${blocksNonSingleModel}")

            return blocksNonSingleModel
        } // .hl7ToJsonModelBlocksNonSingle 
        



        // --------------------------------------------------------------------------------------------------------
        //  ------------- Functions used in the transformation
        // --------------------------------------------------------------------------------------------------------

        private fun getMessageLines(hl7Content: String): List<String> {
            return hl7Content.split("\n")
        } // .getMessageLines

        private fun getObxIdToElNameMap(blocks: List<Block>): Map<String, String> {

            val elems = blocks.flatMap { it.elements } // .mmgElemsBlocksSingle

            return elems.map{ elem -> 
                elem.mappings.hl7v251.identifier to elem.name
            }.toMap()
        } // .getObxIdToElNameMap

        // TODO:
        // private fun getMmgsFiltered(mmgs: Array<MMG>): Array<MMG> {


        // } // .getMmgsFiltered

    } // .companion object
} // .Transformer



    /*
    Element(ordinal=1, name=Message Profile Identifier, dataType=Text, isUnitOfMeasure=false, priority=R, 
    isRepeat=true, repetitions=2, mayRepeat=Y/2, valueSetCode=, valueSetVersionNumber=null, codeSystem=N/A, 
    mappings=Mapping(hl7v251=HL7Mapping(
    legacyIdentifier=NOT115, identifier=N/A: MSH-21, dataType=EI, segmentType=MSH, obrPosition=1, fieldPosition=21, componentPosition=-1, 
    usage=R, cardinality=[2..2], repeatingGroupElementType=NO)))
    */