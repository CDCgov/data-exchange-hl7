package com.example

import com.google.gson.annotations.SerializedName


data class MMG (@SerializedName("result") val result: Result)

data class Result(val id: String, val guideStatus: String, val name: String, val shortName: String, val blocks: List<Block>)

data class Block(
    val id: String,
    val ordinal: Int,
    val type: String,
    val name: String,
    val elements: List<Element>
)

data class Element(
    val ordinal: Int,
    val name: String,
    val dataType: String,
    val isUnitOfMeasure: Boolean,
    val priority: String,
    val isRepeat: Boolean,
    val repetitions: Int,
    val mayRepeat: String,
    val valueSetCode: String?,
    val valueSetVersionNumber: Int?,
    val codeSystem: String?,
    val mappings: Mapping,

)

data class Mapping(
    val hl7v251: HL7Mapping

)

data class HL7Mapping (
    val legacyIdentifier: String,
    val identifier: String,
    val dataType: String,
    val segmentType: String,
    val orbPosition: Int,
    val fieldPosition: Int,
    val componentPosition: Int,
    val usage: String,
    val cardinality: String,
    val repeatingGroupElementType: String

)