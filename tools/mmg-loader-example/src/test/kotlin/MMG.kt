
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MMG (@JsonProperty("result") val result: Result)


@JsonIgnoreProperties(ignoreUnknown = true)
data class Result(val id: String, val guideStatus: String, val name: String, val shortName: String, val blocks: List<Block>)


@JsonIgnoreProperties(ignoreUnknown = true)
data class Block(
    val ordinal: Int,
    val type: String,
    val name: String,
    val elements: List<Element>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Element(
    val ordinal: Int,
    val name: String,
//    val description: String,
    val dataType: String,
    val isUnitOfMeasure: Boolean,
    val priority: String,
    val isRepeat: Boolean,
    val repetitions: Int,
    val mayRepeat: String,
    val mappings: Mapping,
    val valueSetCode: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Mapping(
    val hl7v251: HL7Mapping

)

@JsonIgnoreProperties(ignoreUnknown = true)
data class HL7Mapping (
    val legacyIdentifier: String,
    val identifier: String,
//    val messageContext: String,
    val dataType: String,
    val segmentType: String,
    val orbPosition: Int,
    val fieldPosition: Int,
    val componentPosition: Int,
    val usage: String,
    val cardinality: String,
    val repeatingGroupElementType: String

) {
    val path = when (segmentType) {
             "OBX" -> {
                 var p = "$segmentType[@3.1='${identifier}']-5"
                 if ("CE".equals(dataType) || "CWE".equals(dataType) )
                     p += ".1"
                 else if  ("SN".equals(dataType))
                     p += ".2"
                 p
             }
//              "MSH"| "PID"-> {
//                  val regex = "[A-Z]{3}\\-[0-9]*".toRegex()
//                  val path = regex.find(identifier)
//                  path?.value
//              }
              else ->  {
                  var path = "$segmentType-$fieldPosition"
                  if (componentPosition != -1)
                      path += ".$componentPosition"
                  path
              }
        }
}