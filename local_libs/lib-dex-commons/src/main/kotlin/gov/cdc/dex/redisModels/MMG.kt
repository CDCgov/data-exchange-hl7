package gov.cdc.dex.redisModels
data class MMG (val id: String, val guideStatus: String, val name: String, val shortName: String, var blocks: List<Block>)

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

    ) {
    fun getSegmentPath() =  when (mappings.hl7v251.segmentType) {
        "OBX" -> {
            val obxIdentifier = if (mappings.hl7v251.fieldPosition == 6) { //Units o
                val regex = """[0-9]{5}\-[0-9]""".toRegex()
                regex.find(mappings.hl7v251.identifier)?.value
            } else mappings.hl7v251.identifier
             "${mappings.hl7v251.segmentType}[@3.1='$obxIdentifier']"
        }
        "OBR" -> {
            if (mappings.hl7v251.obrPosition > 0)
                "${mappings.hl7v251.segmentType}[${mappings.hl7v251.obrPosition}]"
            else
                mappings.hl7v251.segmentType
        }

        else -> {
            mappings.hl7v251.segmentType
        }
    }
    fun getValuePath() = when (mappings.hl7v251.segmentType) {
        "OBX" -> {
            var p ="${getSegmentPath()}-${mappings.hl7v251.fieldPosition}"
            p += if ("SN" == mappings.hl7v251.dataType) ".2"
                 else ".1"
            p
        }
        "OBR" -> { //take obrPosition into consideration...
            var path = "${mappings.hl7v251.segmentType}[${mappings.hl7v251.obrPosition}]-${mappings.hl7v251.fieldPosition}"
            if (mappings.hl7v251.componentPosition != -1)
                path += ".${mappings.hl7v251.componentPosition}"
            else if (listOf("CE", "CWE").contains(mappings.hl7v251.dataType)) {
                path += ".1"
            }
            path
        }
        else ->  {
            var path = "${mappings.hl7v251.segmentType}-${mappings.hl7v251.fieldPosition}"
            if (mappings.hl7v251.componentPosition != -1)
                path += ".${mappings.hl7v251.componentPosition}"
            else if (listOf("CE", "CWE").contains(mappings.hl7v251.dataType)) {
                path += ".1"
            }
            path
        }
    }

    fun getDataTypePath(): String {
        val obxIdentifier = if (mappings.hl7v251.fieldPosition == 6) { //Units o
            val regex = """[0-9]{5}\-[0-9]""".toRegex()
            regex.find(mappings.hl7v251.identifier)?.value
        } else mappings.hl7v251.identifier
        return "${mappings.hl7v251.segmentType}[@3.1='${obxIdentifier}']-2"
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
    val obrPosition: Int,
    val fieldPosition: Int,
    val componentPosition: Int,
    val usage: String,
    val cardinality: String,
    val repeatingGroupElementType: String

)