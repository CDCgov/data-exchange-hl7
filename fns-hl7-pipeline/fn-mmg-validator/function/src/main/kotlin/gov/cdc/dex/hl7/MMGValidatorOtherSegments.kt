package gov.cdc.dex.hl7

import gov.cdc.dex.hl7.model.*

import open.HL7PET.tools.HL7StaticParser
import org.slf4j.LoggerFactory
import scala.Option


class MmgValidatorOtherSegments(private val hl7Message: String, private val mmgs: Array<MMG>) {
    private val logger = LoggerFactory.getLogger(MmgValidatorOtherSegments::class.java.simpleName)

    private val OBR_4_1_EPI_ID = "68991-9"


    fun validate(): List<ValidationIssue> {
        // val allBlocks:Int  =  mmgs.map { it.blocks.size }.sum()
        // logger.debug("check other segments started blocks.size: --> $allBlocks")

        val report = mutableListOf<ValidationIssue>()
        
        hl7Message.split("\n").forEachIndexed { lineNum, line -> 
            
            val lineParts = line.split("|")
            val segmentName = lineParts[0]

            when (segmentName) {
                "MSH", "PID" -> {}// nothing to do
                "OBR" -> {
                    val segmentID = lineParts[4].split("^")[0]
                    //logger.info("hl7 segmentName: --> " + segmentName + " -- " + "segmentID: --> " + segmentID)
                    if ( segmentID != OBR_4_1_EPI_ID ) {
                        report += ValidationIssue(
                            category= ValidationIssueCategoryType.WARNING,
                            type= ValidationIssueType.SEGMENT_NOT_IN_MMG,
                            fieldName=segmentName, 
                            hl7Path="N/A",
                            lineNumber=lineNum + 1, 
                            errorMessage= ValidationErrorMessage.SEGMENT_NOT_IN_MMG,
                            message="OBR segment 4.1 does not match the EPI identifier. Expected: ${OBR_4_1_EPI_ID}, Found: ${segmentID}",
                        )
                    }
                } // .OBR
                "OBX" -> {
                    // TODO: check for extra OBX vs. MMG 
                    // TODO:
                    val segmentID = lineParts[3].split("^")[0]
                    //logger.info("hl7 segmentName: --> " + segmentName + " -- " + "segmentID: --> " + segmentID)
                } // .OBX
                else -> {
                    if (segmentName.trim() != "") { // empty line, should not happen 
                        report += ValidationIssue(
                            category= ValidationIssueCategoryType.WARNING,
                            type= ValidationIssueType.SEGMENT_NOT_IN_MMG,
                            fieldName=segmentName, 
                            hl7Path="N/A",
                            lineNumber=lineNum + 1, 
                            errorMessage= ValidationErrorMessage.SEGMENT_NOT_IN_MMG,
                            message="Segment does not match: MSH, PID, OBR, or OBX. Found: ${segmentName}", 
                        )
                    } // .if 
                } // .else
            }

        } // hl7Message.split

        
        return report 
    } // .validate


} // .MmgValidatorOtherSegments