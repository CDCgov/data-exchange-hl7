package com.example 

import com.microsoft.azure.functions.ExecutionContext

class MmgValidator() {


    fun validate(context: ExecutionContext, hl7Message: String, blocks: List<Block>): List<ValidationIssue> {

        val report = mutableListOf<ValidationIssue>()

        blocks.forEach { block -> {
            // TODO:
            context.logger.info("block: --> " + block.name)
        }} // blocks.forEach
        
        return report 
    } // .validate 

    fun checkCardinality(): ValidationIssue {

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

    fun checkDataType(): ValidationIssue {
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

    fun checkVocab(): ValidationIssue {
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