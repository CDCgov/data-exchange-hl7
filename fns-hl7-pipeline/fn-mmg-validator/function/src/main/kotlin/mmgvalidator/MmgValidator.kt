package com.example 

import com.microsoft.azure.functions.ExecutionContext
import open.HL7PET.tools.HL7StaticParser


class MmgValidator(val context: ExecutionContext, val hl7Message: String, val blocks: List<Block>) {

    fun validate(): List<ValidationIssue> {
        context.logger.info("validate started blocks.size: --> " + blocks.size)

        val report = mutableListOf<ValidationIssue>()

        for (block in blocks) {
            
            for (element in block.elements) {

                    val path = when (element.mappings.hl7v251.segmentType) {
                        "OBX" -> {
                            var p = "${element.mappings.hl7v251.segmentType}[@3.1='${element.mappings.hl7v251.identifier}']-5"
                            if ("CE".equals(element.mappings.hl7v251.dataType) || "CWE".equals(element.mappings.hl7v251.dataType) )
                                p += ".1"
                            else if  ("SN".equals(element.mappings.hl7v251.dataType))
                                p += ".2"
                            p
                        }
                        else ->  {
                            var path = "$element.mappings.hl7v251.segmentType-${element.mappings.hl7v251.fieldPosition}"
                            if (element.mappings.hl7v251.componentPosition != -1)
                                path += ".${element.mappings.hl7v251.componentPosition}"
                            path
                        }
                    } // .when
                    
                    val msgValues = HL7StaticParser.getValue(hl7Message, path)

                    if (msgValues.isDefined && msgValues?.get() != null) {

                        context.logger.info("msgValues: --> " + msgValues)

                        //checkDataType()
                        // checkVocab() 
                        // checkCardinality()


                    } else { 
                        val vi = checkCardinality()
                        if (vi != null) {
                            report.add(vi)
                        } // .if

                    } // .else
                    


            } // .for element

        } // .for block
        
        return report 
    } // .validate 

    fun checkCardinality(): ValidationIssue? {

        // TODO:
        val vi = ValidationIssue(
            category=ValidationIssueCategoryType.ERROR, 
            type=ValidationIssueType.CARDINALITY, 
            fieldName="fieldName", 
            hl7Path="hl7Path", 
            lineNumber=1,
            errorMessage=ValidationErrorMessage.CARDINALITY_NOT_FOUND, // CARDINALITY_OVER
            message="Element mmg cardinality is: ... , found cardinality is: ...",
        ) // .ValidationIssue
        return vi 
    } // .checkCardinality 

    fun checkDataType(): ValidationIssue? {
        // TODO:
        val vi = ValidationIssue(
            category=ValidationIssueCategoryType.ERROR, 
            type=ValidationIssueType.DATA_TYPE, 
            fieldName="fieldName", 
            hl7Path="hl7Path", 
            lineNumber=1,
            errorMessage=ValidationErrorMessage.DATA_TYPE_NOT_FOUND, // DATA_TYPE_MISMATCH
            message="Element mmg cardinality is: ... , found cardinality is: ...",
        ) // .ValidationIssue
        return vi 
    } // .checkDataType

    fun checkVocab(): ValidationIssue? {
        // TODO:
        val vi = ValidationIssue(
            category=ValidationIssueCategoryType.ERROR, 
            type=ValidationIssueType.VOCAB, 
            fieldName="fieldName", 
            hl7Path="hl7Path", 
            lineNumber=1,
            errorMessage=ValidationErrorMessage.VOCAB_NOT_AVAILABLE, // VOCAB_ISSUE
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


} // .MmgValidator