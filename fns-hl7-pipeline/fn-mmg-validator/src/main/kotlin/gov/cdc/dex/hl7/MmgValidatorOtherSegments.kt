package gov.cdc.dex.hl7

import gov.cdc.dex.hl7.model.*
import gov.cdc.dex.redisModels.Element
import org.slf4j.LoggerFactory
import gov.cdc.dex.redisModels.MMG


class MmgValidatorOtherSegments(private val hl7Message: String, private val mmgs: Array<MMG>) {
    private val logger = LoggerFactory.getLogger(MmgValidatorOtherSegments::class.java.simpleName)
    private val OBR_4_1_EPI_ID = "68991-9"

    fun validateOtherSegments(): List<ValidationIssue> {
        // val allBlocks:Int  =  mmgs.map { it.blocks.size }.sum()
        // logger.debug("check other segments started blocks.size: --> $allBlocks")

        val elementsByObxID = makeMMGsMapByObxID()
        val report = mutableListOf<ValidationIssue>()
        hl7Message.split("\n").forEachIndexed { lineNum, line ->
            
            val lineParts = line.split("|")
            val segmentName = lineParts[0]

            when (segmentName) {
                "MSH", "PID", "OBR" -> {}// nothing to do
//                "OBR" -> {
//                    val segmentID = lineParts[4].split("^")[0]
//                    //logger.info("hl7 segmentName: --> " + segmentName + " -- " + "segmentID: --> " + segmentID)
//                    if ( segmentID != OBR_4_1_EPI_ID ) {
//                        report += ValidationIssue(
//                            category= ValidationIssueCategoryType.WARNING,
//                            type= ValidationIssueType.SEGMENT_NOT_IN_MMG,
//                            fieldName=segmentName,
//                            hl7Path="N/A",
//                            lineNumber=lineNum + 1,
//                            errorMessage= ValidationErrorMessage.SEGMENT_NOT_IN_MMG,
//                            message="OBR segment 4.1 does not match the EPI identifier. Expected: ${OBR_4_1_EPI_ID}, Found: ${segmentID}",
//                        )
//                    }
//                } // .OBR
                "OBX" -> {
                    val segmentID = lineParts[3].split("^")[0]

                    if ( !elementsByObxID.containsKey(segmentID) ) {
                        report += ValidationIssue(
                                classification= ValidationIssueCategoryType.WARNING,
                                category= ValidationIssueType.SEGMENT_NOT_IN_MMG,
                                fieldName=segmentName, 
                                path ="N/A",
                                line=lineNum + 1,
                                errorMessage= ValidationErrorMessage.SEGMENT_NOT_IN_MMG,
                                description="OBX segment identifier not found in the MMG. Found: ${segmentID}",
                            )
                    } // .if 
                    //logger.info("hl7 segmentName: --> " + segmentName + " -- " + "segmentID: --> " + segmentID)
                } // .OBX
//                else -> {
//                    if (segmentName.trim() != "") { // empty line, should not happen
//                        report += ValidationIssue(
//                            classification= ValidationIssueCategoryType.WARNING,
//                            category= ValidationIssueType.SEGMENT_NOT_IN_MMG,
//                            fieldName=segmentName,
//                            path="N/A",
//                            line=lineNum + 1,
//                            errorMessage= ValidationErrorMessage.SEGMENT_NOT_IN_MMG,
//                            description="Segment does not match: MSH, PID, OBR, or OBX. Found: ${segmentName}",
//                        )
//                    } // .if
//                } // .else
            }

        } // hl7Message.split

        
        return report 
    } // .validate

    
    fun makeMMGsMapByObxID(): Map<String, Element> {

        val elementsByObxID = mutableMapOf<String, Element>()

        mmgs.forEach { mmg ->
            mmg.blocks.forEach { block ->
                block.elements.forEach { element ->
                    elementsByObxID[element.mappings.hl7v251.identifier] = element
                } // .for element
            } // .for block
        }// .for mmg

        return elementsByObxID.toMap()
    } // .makeMMGsMapByObxID


} // .MmgValidatorOtherSegments