package gov.cdc.dex.hl7

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.dex.hl7.model.*

import open.HL7PET.tools.HL7StaticParser
import scala.Option


class MmgValidator(val context: ExecutionContext, val hl7Message: String, val mmgs: Array<MMG>) {

    fun validate(): List<ValidationIssue> {
        val allBlocks:Int  =  mmgs.map { it.blocks.size }.sum()
        context.logger.info("validate started blocks.size: --> " + allBlocks)
//
        val report = mutableListOf<ValidationIssue>()
        mmgs.forEach { mmg ->
            mmg.blocks.forEach { block ->
                block.elements.forEach { element ->
                    val msgValues = HL7StaticParser.getValue(hl7Message, element.getValuePath())
                    val valueList = if(msgValues.isDefined)
                        msgValues.get().flatten()
                    else listOf()
                    //Cardinality Check!
                    checkCardinality(element, valueList, report)
                    // Data type check:
                    if ("OBX" == element.mappings.hl7v251.segmentType) {
                        val msgDataType = HL7StaticParser.getFirstValue(hl7Message, element.getDataTypePath())
                        checkDataType(element, msgDataType, report)
                    }
                   // TODO: Vocab Check
                    // checkVocab()
                } // .for element
            } // .for block
        }
        
        return report 
    } // .validate




    private fun checkCardinality(element: Element, msgValues: List<String>, vr:MutableList<ValidationIssue>) {
        val cardinality = element.mappings.hl7v251.cardinality
        val card1Re = """\d+|\*""".toRegex()
        val cards = card1Re.findAll(cardinality)
        val card1 = cards.elementAt(0).value
        val card2 = cards.elementAt(1).value

        when (card1) {
            "0" ->  "free pass"
            else -> if (msgValues.size < card1.toInt()) {
                vr += ValidationIssue(
                    category= ValidationIssueCategoryType.ERROR,
                    type= ValidationIssueType.CARDINALITY,
                    fieldName=element.name,
                    hl7Path=element.getValuePath(),
                    lineNumber=1,
                    errorMessage= ValidationErrorMessage.CARDINALITY_NOT_FOUND, // CARDINALITY_OVER
                    message="Minimum required value not present. Requires $card1, Found ${msgValues.size}",
                ) // .ValidationIssue
            }
        }
        when (card2) {
            "*" -> "Unbounded"
            else -> if (msgValues.size > card2.toInt()) {
                vr += ValidationIssue(
                    category= ValidationIssueCategoryType.ERROR,
                    type= ValidationIssueType.CARDINALITY,
                    fieldName="fieldName",
                    hl7Path="hl7Path",
                    lineNumber=1,
                    errorMessage= ValidationErrorMessage.CARDINALITY_NOT_FOUND, // CARDINALITY_OVER
                    message="Maximum values surpassed requirements. Max allowed: $card2, Found ${msgValues.size}",
                ) // .ValidationIssue
            }
        }
    } // .checkCardinality 

    private fun checkDataType(element: Element, msgDataType: Option<String>?, report: MutableList<ValidationIssue>) {
        if (msgDataType != null) {
            if (!msgDataType.isDefined || msgDataType.get() != element.mappings.hl7v251.dataType)
                report += ValidationIssue(
                    category= ValidationIssueCategoryType.ERROR,
                    type= ValidationIssueType.DATA_TYPE,
                    fieldName=element.name,
                    hl7Path=element.getDataTypePath(),
                    lineNumber=1,
                    errorMessage= ValidationErrorMessage.DATA_TYPE_NOT_FOUND, // DATA_TYPE_MISMATCH
                    message="Data type on message does not match expected data type on MMG. Expected: ${element.mappings.hl7v251.dataType}, Found: ${msgDataType.get()} ",
                )
        }

    } // .checkDataType

    fun checkVocab(): ValidationIssue? {
        // TODO:
        val vi = ValidationIssue(
            category= ValidationIssueCategoryType.ERROR,
            type= ValidationIssueType.VOCAB,
            fieldName="fieldName", 
            hl7Path="hl7Path", 
            lineNumber=1,
            errorMessage= ValidationErrorMessage.VOCAB_NOT_AVAILABLE, // VOCAB_ISSUE
            message="Element mmg cardinality is: ... , found cardinality is: ...",
        ) // .ValidationIssue
        return vi 
    } // .checkVocab 

    fun getCategory(usage: String): String {
        return when (usage) {
            "R" -> "ERROR"
            else -> "WARNING"
        }
    } // .getCategory

    operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)
} // .MmgValidator