package gov.cdc.dex.hl7

import gov.cdc.dex.hl7.exception.InvalidConceptKey
import gov.cdc.dex.hl7.model.*
import gov.cdc.hl7.HL7StaticParser


import org.slf4j.LoggerFactory
import scala.Option
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis

class MmgValidator(private val hl7Message: String, private val mmgs: Array<MMG>) {
    companion object {
        val REDIS_CACHE_NAME = System.getenv("REDIS_CACHE_NAME")
        val REDIS_PWD =        System.getenv("REDIS_CACHE_KEY")
    }
    val jedis = Jedis(REDIS_CACHE_NAME, 6380, DefaultJedisClientConfig.builder()
        .password(REDIS_PWD)
        .ssl(true)
        .build()
    )
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
                    val msgSegments = HL7StaticParser.getValue(hl7Message, element.getSegmentPath())
                    val valueList = if(msgSegments.isDefined)
                        msgSegments.get().flatten()
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
                    if (msgSegments.isDefined)  {
                        val msgValues = HL7StaticParser.getValue(hl7Message, element.getValuePath())
                        if (msgValues.isDefined)
                            checkVocab(element,msgValues.get(), hl7Message, report)
                    }

                } // .for element
            } // .for block
        }// .for mmg
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
                    checkSingleGroupCardinaltiy(minCardinality, maxCardinality, groupID, element, groupOBX, report)
                }
            }
        } else {
            val allSegs = msgValues.joinToString("\n") //join all segments to extract all Values.
            val segValues = HL7StaticParser.getValue(allSegs, element.getValuePath())
//            val segValuesFlat = if (segValues.isDefined) segValues.get().flatten() else listOf()
            checkSingleGroupCardinaltiy(minCardinality, maxCardinality, null, element, segValues, report)

        }
    }
    private fun checkSingleGroupCardinaltiy(minCardinality: String, maxCardinality: String, groupID: String?,  element: Element, matchingSegs: Option<Array<Array<String>>>, report: MutableList<ValidationIssue>) {
        val values = if (matchingSegs.isDefined) matchingSegs.get().flatten() else listOf()
        if (minCardinality.toInt() > 0 && values.distinct().size < minCardinality.toInt()) {
            val matchingSegments = HL7StaticParser.getListOfMatchingSegments(hl7Message, element.mappings.hl7v251.segmentType, getSegIdx(element))
            val subList = if (groupID != null) {
                matchingSegments.filter { it._2[4] == groupID}
            } else matchingSegments
            report += ValidationIssue(
                category= getCategory(element.mappings.hl7v251.usage),
                type= ValidationIssueType.CARDINALITY,
                fieldName=element.name,
                hl7Path=element.getValuePath(),
                lineNumber=subList.keys().toList().last().toString().toInt(), //Get the last Occurrence of line number
                errorMessage= ValidationErrorMessage.CARDINALITY_UNDER, // CARDINALITY_OVER
                message="Minimum required value not present. Requires $minCardinality, Found ${values.size}",
            ) // .ValidationIssue
        }

        when (maxCardinality) {
            "*" -> "Unbounded"
            else -> if (values.distinct().size > maxCardinality.toInt()) {
                val matchingSegments = HL7StaticParser.getListOfMatchingSegments(hl7Message, element.mappings.hl7v251.segmentType, getSegIdx(element))
                val subList = if (groupID != null) {
                     matchingSegments.filter { it._2[4] == groupID}
                } else matchingSegments
                report += ValidationIssue(
                    category= getCategory(element.mappings.hl7v251.usage),
                    type= ValidationIssueType.CARDINALITY,
                    fieldName=element.name,
                    hl7Path=element.getValuePath(),
                    lineNumber=subList.keys().toList().last().toString().toInt(),
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

    private fun checkVocab(elem: Element, msgValues: Array<Array<String>>, message: String, report:MutableList<ValidationIssue> ) {
        if (!elem.valueSetCode.isNullOrEmpty() && !"N/A".equals(elem.valueSetCode)) {
            logger.debug("Validating ${elem.valueSetCode}")
            //val concepts = retrieveValueSetConcepts(elem.valueSetCode)
            msgValues.forEachIndexed { outIdx, outArray ->
                outArray.forEachIndexed { inIdx, inElem ->
                    //if (concepts.filter { it.conceptCode == inElem }.isEmpty()) {
                    if (!isConceptValid2(elem.valueSetCode, inElem)) {
                        val lineNbr = getLineNumber(message, elem, outIdx)
                        val issue = ValidationIssue(
                            getCategory(elem.mappings.hl7v251.usage),
                            ValidationIssueType.VOCAB,
                            elem.name,
                            elem.getValuePath(),
                            lineNbr,
                            ValidationErrorMessage.VOCAB_ISSUE,
                            "Unable to find '$inElem' on '${elem.valueSetCode}' on line $lineNbr"
                        )
                        report.add(issue)
                    }
                }//.forEach Inner Array
            } //.forEach Outer Array
        }
    }

    //Some Look ps are reused - storing them so no need to re-download them from Redis.
    private val valueSetMap = mutableMapOf<String, List<String>>()
    //    private val mapper = jacksonObjectMapper()
    @Throws(InvalidConceptKey::class)
    fun isConceptValid2(key: String, concept: String): Boolean {
        if (valueSetMap[key] == null) {
            logger.debug("Retrieving $key from Redis")
            val conceptStr = jedis.hgetAll(key) ?: throw InvalidConceptKey("Unable to retrieve concept values for $key")
//            val listType = object : TypeToken<List<ValueSetConcept>>() {}.type
            valueSetMap[key] = conceptStr.keys.toList()

        }
        return valueSetMap[key]?.filter { it == concept }?.isNotEmpty() ?: false
    }
    fun isConceptValid(key: String, concept: String):Boolean {
        //TODO::Check if Key is truly valid (configuration issue)
        return jedis.hexists(key, concept)
    }

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