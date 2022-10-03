package gov.cdc.dex.hl7

import gov.cdc.dex.hl7.model.*

import open.HL7PET.tools.HL7StaticParser
import org.slf4j.LoggerFactory
import scala.Option


class MmgValidator(private val hl7Message: String, private val mmgs: Array<MMG>) {
    private val logger = LoggerFactory.getLogger(MmgValidator::class.java.simpleName)
    fun validate(): List<ValidationIssue> {
        val allBlocks:Int  =  mmgs.map { it.blocks.size }.sum()
        logger.debug("validate started blocks.size: --> $allBlocks")
//
        val report = mutableListOf<ValidationIssue>()
        mmgs.forEach { mmg ->
            mmg.blocks.forEach { block ->
                block.elements.forEach { element ->
                    //TODO:: DO not validate GenV2 MSH-21 if you have a Condition Specific MMG.

                    //Cardinality Check!
                    val msgValues = HL7StaticParser.getValue(hl7Message, element.getSegmentPath())
                    val valueList = if(msgValues.isDefined)
                        msgValues.get().flatten()
                    else listOf()
                    checkCardinality(block.type in listOf("Repeat", "RepeatParentChild"), element, valueList, report)
                    // Data type check: (Don't check Data type for Units of measure - fieldPosition is 6, not 5 - can't use isUnitOfMeasure field.)
                    if ("OBX" == element.mappings.hl7v251.segmentType && 5 == element.mappings.hl7v251.fieldPosition) {
                        val dataTypeSegments = HL7StaticParser.getListOfMatchingSegments(hl7Message, element.mappings.hl7v251.segmentType, getSegIdx(element))
                        for ( k in dataTypeSegments.keys().toList()) {
                           checkDataType(hl7Message, element, dataTypeSegments[k].get()[2], k.toString().toInt(), report )
                        }
                    }
                   // TODO: Vocab Check
                    // checkVocab()
                } // .for element
            } // .for block
        }// .for mmg
        //TODO::Check for extra OBXs
//        checkExtraOBX()
        
        return report 
    } // .validate




    private fun checkCardinality(blockRepeat: Boolean, element: Element, msgValues: List<String>, report:MutableList<ValidationIssue>) {
        val cardinality = element.mappings.hl7v251.cardinality
        val card1Re = """\d+|\*""".toRegex()
        val cards = card1Re.findAll(cardinality)
        val minCardinality = cards.elementAt(0).value
        val maxCardinality = cards.elementAt(1).value

        if (blockRepeat) { //cardinality must be checked within Blocks of OBX-4
            val allOBXs = msgValues.joinToString("\n")
            val uniqueGroups = HL7StaticParser.getValue(allOBXs, "OBX-4")
            if (uniqueGroups.isDefined) {
                uniqueGroups.get().flatten().distinct().forEach { groupID ->
                    val groupOBX = HL7StaticParser.getValue(allOBXs, "OBX[@4='$groupID']-5")
                    checkSingleGroupCardinaltiy(minCardinality, maxCardinality, element, groupOBX.get().flatten(), report)
                }
            }
        } else {
            val allSegs = msgValues.joinToString("\n") //join all segments to extract all Values.
            val segValues = HL7StaticParser.getValue(allSegs, element.getValuePath())
            val segValuesFlat = if (segValues.isDefined) segValues.get().flatten() else listOf()
            checkSingleGroupCardinaltiy(minCardinality, maxCardinality, element, segValuesFlat, report)

        }
    }
    private fun checkSingleGroupCardinaltiy(minCardinality: String, maxCardinality: String, element: Element, values: List<String>, report: MutableList<ValidationIssue>) {
        if (minCardinality.toInt() > 0 && values.size < minCardinality.toInt()) {
            val matchingSegments = HL7StaticParser.getListOfMatchingSegments(hl7Message, element.mappings.hl7v251.segmentType, getSegIdx(element))
            report += ValidationIssue(
                category= getCategory(element.mappings.hl7v251.usage),
                type= ValidationIssueType.CARDINALITY,
                fieldName=element.name,
                hl7Path=element.getValuePath(),
                lineNumber=matchingSegments.keys().toList().last().toString().toInt(), //Get the last Occurrence of line number
                errorMessage= ValidationErrorMessage.CARDINALITY_UNDER, // CARDINALITY_OVER
                message="Minimum required value not present. Requires $minCardinality, Found ${values.size}",
            ) // .ValidationIssue
        }

        when (maxCardinality) {
            "*" -> "Unbounded"
            else -> if (values.size > maxCardinality.toInt()) {
                val matchingSegments = HL7StaticParser.getListOfMatchingSegments(hl7Message, element.mappings.hl7v251.segmentType, getSegIdx(element))
                report += ValidationIssue(
                    category= getCategory(element.mappings.hl7v251.usage),
                    type= ValidationIssueType.CARDINALITY,
                    fieldName=element.name,
                    hl7Path=element.getValuePath(),
                    lineNumber=matchingSegments.keys().toList().last().toString().toInt(),
                    errorMessage= ValidationErrorMessage.CARDINALITY_OVER, // CARDINALITY_OVER
                    message="Maximum values surpassed requirements. Max allowed: $maxCardinality, Found ${values.size}",
                ) // .ValidationIssue
            }
        }
    } // .checkCardinality 

    private fun checkDataType(message: String, element: Element, msgDataType: String?, lineNbr: Int, report: MutableList<ValidationIssue>) {
        if (msgDataType != null && msgDataType != element.mappings.hl7v251.dataType) {
                report += ValidationIssue(
                    category= getCategory(element.mappings.hl7v251.usage),
                    type= ValidationIssueType.DATA_TYPE,
                    fieldName=element.name,
                    hl7Path=element.getDataTypePath(),
                    lineNumber=lineNbr, //Data types only have single value.
                    errorMessage= ValidationErrorMessage.DATA_TYPE_MISMATCH, // DATA_TYPE_MISMATCH
                    message="Data type on message does not match expected data type on MMG. Expected: ${element.mappings.hl7v251.dataType}, Found: ${msgDataType}",
                )
        }

    } // .checkDataType

    fun checkVocab(report: MutableList<ValidationIssue>) {
        // TODO:
//        val vi = ValidationIssue(
//            category= getCategory(element.mappings.hl7v251.usage),
//            type= ValidationIssueType.VOCAB,
//            fieldName="fieldName",
//            hl7Path="hl7Path",
//            lineNumber=1,
//            errorMessage= ValidationErrorMessage.VOCAB_NOT_AVAILABLE, // VOCAB_ISSUE
//            message="Element mmg cardinality is: ... , found cardinality is: ...",
//        ) // .ValidationIssue

    } // .checkVocab 

    private fun getCategory(usage: String): ValidationIssueCategoryType {
        return when (usage) {
            "R" -> ValidationIssueCategoryType.ERROR
            else -> ValidationIssueCategoryType.WARNING
        }
    } // .getCategory

    private fun getSegIdx(elem: Element): String {
        return when (elem.mappings.hl7v251.segmentType) {
            "OBX" -> "@3.1='${elem.mappings.hl7v251.identifier}'"
            else -> "1"
        }
    }
    private fun getLineNumber(message: String, elem: Element, outArrayIndex: Int): Int {
        val allSegs = HL7StaticParser.getListOfMatchingSegments(message, elem.mappings.hl7v251.segmentType, getSegIdx(elem))
        var line = 0
        var forBreak = 0
        for ( k in allSegs.keys().toList()) {
            line = k as Int
            if (forBreak >= outArrayIndex) break
            forBreak++
        }
        return line
    }

} // .MmgValidator