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

){
    val path = when (mappings.hl7v251.segmentType) {
        "OBX" -> {
            var p = "${mappings.hl7v251.segmentType}[@3.1='${mappings.hl7v251.identifier}']-5"
            if ("CE".equals(mappings.hl7v251.dataType) || "CWE".equals(mappings.hl7v251.dataType) )
                p += ".1"
            else if  ("SN".equals(mappings.hl7v251.dataType))
                p += ".2"
            p
        }
//              "MSH"| "PID"-> {
//                  val regex = "[A-Z]{3}\\-[0-9]*".toRegex()
//                  val path = regex.find(identifier)
//                  path?.value
//              }
        else ->  {
            var path = "$mappings.hl7v251.segmentType-${mappings.hl7v251.fieldPosition}"
            if (mappings.hl7v251.componentPosition != -1)
                path += ".${mappings.hl7v251.componentPosition}"
            path
        }
    }

}

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