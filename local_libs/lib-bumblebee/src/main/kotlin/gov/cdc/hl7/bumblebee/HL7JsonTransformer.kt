package gov.cdc.hl7.bumblebee

import com.google.gson.*
import gov.cdc.hl7.HL7ParseUtils
import gov.cdc.hl7.bumblebee.StringUtils.Companion.normalize
import gov.cdc.hl7.model.HL7Hierarchy


class HL7JsonTransformer(val profile: Profile, val fieldProfile: Profile, val hl7Parser: HL7ParseUtils) {
    companion object {
        val gson = GsonBuilder().serializeNulls().create()
        //Factory Method
        @JvmStatic
        fun getTransformerWithResource(
            message: String,
            profileFilename: String,
            fieldProfileFileName: String = "/DefaultFieldsProfileSimple.json"
        ): HL7JsonTransformer {
            val profContent = HL7JsonTransformer::class.java.getResource("/$profileFilename").readText()
            val profile: Profile = gson.fromJson(profContent, Profile::class.java)

            val fieldProfContent = HL7JsonTransformer::class.java.getResource(fieldProfileFileName).readText()
            val fieldProfile: Profile = gson.fromJson(fieldProfContent, Profile::class.java)

            val parser = HL7ParseUtils.getParser(message, profileFilename)
            return HL7JsonTransformer(profile, fieldProfile, parser)
        }
    }

    fun transformMessage(): JsonObject {
        val fullHL7 = JsonObject()
        val msg = hl7Parser.msgHierarchy()

        msg.children().foreach {
            processMsgSeg(it, fullHL7)
        }
        //Fix MSH-1 and 2:
        val msh =fullHL7.get("MSH").asJsonObject
        msh.addProperty("field_separator", "|")
        msh.addProperty("encoding_characters", "^~\\&")
        return fullHL7

    }

    private fun getValueFromMessage(arrayVal: List<String>?, fieldNbr: Int, fieldIndexSkew: Int = 0): String? {
        return if (arrayVal!= null && arrayVal.size > (fieldNbr - fieldIndexSkew)) arrayVal[fieldNbr - fieldIndexSkew] else null
    }

    private fun processMsgSeg(seg: HL7Hierarchy, parentJson: JsonElement) {
        //Prepare Json Node for Segment:
        val segID = seg.segment().substring(0,3)
        val segJson = JsonObject()
        if (parentJson.isJsonObject)
            parentJson.asJsonObject.add(segID, segJson)
        else {
            val segArrayJson = JsonObject()
            segArrayJson.add(segID, segJson)
            parentJson.asJsonArray.add(segArrayJson)
        }
        //Prepare elements of this segment
        val segArray = seg.segment().split("|")
//        val fieldIndexSkew = if (segID == "MSH") 1 else 0
        profile.getSegmentField(segID)?.forEach { segField ->
            //Add A JsonObject if max cardinality is 1, array otherwise.
            var fieldJsonNode = if (getCardinality(segField.cardinality) == "1")
                JsonObject()
            else {
                JsonArray()
            } //Adding empty array to field with cardinality > 1
            segJson.add(segField.name.normalize(), fieldJsonNode)

            //Get the value of this field from Message....
            val fieldVal = getValueFromMessage(segArray, segField.fieldNumber, if (segID == "MSH") 1 else 0)
            //Is this field defined with components?
            //For OBX-5, use OBX 2 as the data type. Everything else, use segField
            val dataTypeToUse = if (segID == "OBX" && segField.fieldNumber == 5) segArray[2] else segField.dataType
            val components =  fieldProfile.getSegmentField(dataTypeToUse)
            val fieldRepeat = fieldVal?.split("~")
            if (components == null) { //No components - it's primitive, just add value!
                if (fieldJsonNode.isJsonObject) {
                    segJson.addValueOrNull(fieldRepeat?.get(0), segField.name)
                    fieldJsonNode.asJsonObject.addProperty(segField.name.normalize(), fieldRepeat?.get(0))
                } else {
                    if (fieldRepeat == null || fieldRepeat[0].isEmpty()) {
                        segJson.add(segField.name.normalize(), JsonNull.INSTANCE)
                    } else {
                        fieldRepeat.forEach { fieldRepeatItem ->
                            fieldJsonNode.asJsonArray.add(fieldRepeatItem)
                        }
                    }

                }
            } else {
                if (fieldRepeat == null || fieldRepeat[0].isEmpty()) {
                    segJson.add(segField.name.normalize(), JsonNull.INSTANCE)
                }
                else fieldRepeat.forEach { fieldRepeatItem ->
                    val compJsonObj = JsonObject()
                    val compArray = fieldRepeatItem.split("^")
                    var compHasValue:Boolean = false
                    components.forEach { component ->
                        val compVal = getValueFromMessage(compArray, component.fieldNumber -1 )
                        compHasValue = compHasValue || (!compVal.isNullOrEmpty() && compVal.replace("&", "").trim() != null)
                        //Handle subcomponents...
                        val subComponents = fieldProfile.getSegmentField(component.dataType)
                        if (!subComponents.isNullOrEmpty()) {
                            val subCompJsonObj = JsonObject()
                            val subCompArray = compVal?.split("&")
                            var subHasValue: Boolean = false
                            subComponents.forEach { subComp ->
                                val subCompVal = getValueFromMessage(subCompArray, subComp.fieldNumber - 1)
                                subCompJsonObj.addValueOrNull(subCompVal, subComp.name)
                                subHasValue = subHasValue || !subCompVal.isNullOrEmpty()
                            }
                            if (subHasValue)
                                compJsonObj.add(component.name.normalize(), subCompJsonObj)
                            else
                                compJsonObj.add(component.name.normalize(), JsonNull.INSTANCE)
                        } else {
                            compJsonObj.addValueOrNull(compVal, component.name)
                        }
                    }
                    if (fieldJsonNode.isJsonArray && compHasValue)
                        fieldJsonNode.asJsonArray.add(compJsonObj)
                    else {
                        if (compHasValue) {
                            segJson.add(segField.name.normalize(), compJsonObj)
                            fieldJsonNode = compJsonObj
                        } else
                            segJson.add(segField.name.normalize(), JsonNull.INSTANCE)
                    }
                }
            }
            //Fix empty JsonNode if no values were found.
            if ((fieldJsonNode.isJsonObject && fieldJsonNode.asJsonObject.size() == 0))
                segJson.add(segField.name.normalize(), JsonNull.INSTANCE)

        }
        if (!seg.children().isEmpty) {
            val childArray = JsonArray()
            segJson.add("children", childArray)
            seg.children().foreach { childSeg ->
                processMsgSeg(childSeg, childArray)
            }
        }
    }

    private fun getCardinality(cardinality: String): String {
        val end = try {
            cardinality.substring(cardinality.indexOf("..")+2, cardinality.length -1)
        }catch (e: StringIndexOutOfBoundsException) {
            "UNK" }
        return if (end == "*") "*"
        else
            try {
                "${end.toInt()}"
            } catch (e: NumberFormatException) {
                "?"
            }
    }

    fun JsonObject.addValueOrNull(value: String?, name: String) {
        if (!value.isNullOrEmpty())
            this.addProperty(name.normalize(), value)
        else this.add(name.normalize(),JsonNull.INSTANCE)
    }
}
