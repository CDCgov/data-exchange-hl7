package gov.cdc.hl7.bumblebee

import com.google.gson.*
import gov.cdc.hl7.HL7ParseUtils
import gov.cdc.hl7.model.HL7Hierarchy

enum class  CARDINALITY_ENUM {
    CARDINALITY_UNK, CARDINALITY_MANY
}

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
        msh.addProperty("file_separator", "|")
        msh.addProperty("encoding_characters", "^~\\&")

        //Profile as Pivot
//        profile.segmentDefinition?.forEach {
//            processSegment(null, it.key, it.value, fullHL7)
//        }
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
            val fieldJsonNode = if (getCardinality(segField.cardinality) == "1")
                JsonObject()
            else {
                JsonArray()
            }
            segJson.add(segField.name.normalize(), fieldJsonNode)

            //Get the value of this field from Message....
            val fieldVal = getValueFromMessage(segArray, segField.fieldNumber, if (segID == "MSH") 1 else 0)
            //Is this field defined with components?
            //For OBX-5, use OBX 2 as the data type. Everything else, use segField
            val dataTypeToUse = if (segID == "OBX" && segField.fieldNumber == 5) segArray[2] else segField.dataType
            val components =  fieldProfile.getSegmentField(dataTypeToUse)
            val fieldRepeat = fieldVal?.split("~")
            if (components == null) { //No components - it's primitive, just add value!
                if (fieldJsonNode.isJsonObject)
                    segJson.addValueOrNull(fieldRepeat?.get(0), segField.name)
                else
                    if (fieldRepeat != null && fieldRepeat[0].isNotEmpty()) {
                        fieldJsonNode.asJsonArray.add(fieldRepeat[0])
                    }
            } else {
                fieldRepeat?.forEach { fieldRepeatItem ->
                    val compJsonObj = JsonObject()
                    val compArray = fieldRepeatItem.split("^")
                    components.forEach { component ->
                        val compVal = getValueFromMessage(compArray, component.fieldNumber -1 )
                           // if (compArray.size > component.fieldNumber - 1) compArray[component.fieldNumber - 1] else null
                        //Handle subcomponents...
                        val subComponents = fieldProfile.getSegmentField(component.dataType)
                        if (!subComponents.isNullOrEmpty()) {
                            val subCompArray = compVal?.split("&")
                            val subCompJsonObj = JsonObject()
                            subComponents.forEach { subComp ->
                                val subCompVal = getValueFromMessage(subCompArray, subComp.fieldNumber - 1)
                                   // if (subCompArray != null && subCompArray.size > subComp.fieldNumber - 1) subCompArray[subComp.fieldNumber - 1] else null
                                subCompJsonObj.addValueOrNull(subCompVal, subComp.name)
                            }
                            compJsonObj.add(component.name.normalize(), subCompJsonObj)
                        } else {
                            compJsonObj.addValueOrNull(compVal, component.name)
                        }
                    }
                    if (fieldJsonNode.isJsonArray)
                        fieldJsonNode.asJsonArray.add(compJsonObj)
                    else
                        segJson.add(segField.name.normalize(), compJsonObj)
                }
            }


        }
        if (!seg.children().isEmpty) {//Do not change to isNotEmpty.
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



    private fun processSegment(parentSeg: String?, seg: String, segConfig: SegmentConfig, parentJson: JsonElement) {
        //Does message has this seg?

        val segMsg = if (parentSeg == null) hl7Parser.getValue(seg)
        else hl7Parser.getValue("$parentSeg->$seg")

        if (segMsg.isDefined && segMsg.get().size > 0) {
            segMsg.get().flatten().forEachIndexed { segIndex, segValue ->
                val segJson = JsonObject()
                if (parentJson.isJsonObject)
                    parentJson.asJsonObject.add(seg, segJson)
                else {
                    val segArrayJson = JsonObject()
                    segArrayJson.add(seg, segJson)
                    parentJson.asJsonArray.add(segArrayJson)
                }
                val segArray = segValue.split("|")
                val fieldIndexSkew = if (seg == "MSH") 1 else 0
                profile.getSegmentField(seg)?.forEach { segField ->
                    val fieldVal =
                        if (segArray.size > (segField.fieldNumber - fieldIndexSkew)) segArray[segField.fieldNumber - fieldIndexSkew] else null

                    val fieldRepeat = fieldVal?.split("~")
                    val jsonArrayValues: JsonArray? =
                        if (fieldRepeat != null && fieldRepeat.size > 1) JsonArray() else null
                    if (fieldRepeat != null && fieldRepeat.size > 1)
                        segJson.add(segField.name, jsonArrayValues)
                    val components = fieldProfile.getSegmentField(segField.dataType)
                    fieldRepeat?.forEach { fieldRepeatItem ->
                        val segFieldObj = if (fieldRepeat.size == 1)
                            segJson else JsonObject()

                        if (components == null) {
                            //It's a primitive, assign value:
                            segFieldObj.addValueOrNull(fieldRepeatItem, segField.name )
                            jsonArrayValues?.add(segFieldObj)
                        } else {
                            val compJsonObj = JsonObject()
                            val compArray = fieldRepeatItem.split("^")
                            components.forEach { component ->
                                val compVal =
                                    if (compArray.size > component.fieldNumber - 1) compArray[component.fieldNumber - 1] else null
                                //Handle subcomponents...
                                val subComponents = fieldProfile.getSegmentField(component.dataType)
                                if (!subComponents.isNullOrEmpty()) {
                                    val subCompArray = compVal?.split("&")
                                    val subCompJsonObj = JsonObject()
                                    subComponents.forEach { subComp ->
                                        val subCompVal =
                                            if (subCompArray != null && subCompArray.size > subComp.fieldNumber - 1) subCompArray[subComp.fieldNumber - 1] else null
                                        subCompJsonObj.addValueOrNull(subCompVal, subComp.name)
                                    }
                                    compJsonObj.add(component.name, subCompJsonObj)
                                } else {
                                    compJsonObj.addValueOrNull(compVal, component.name)
                                }
                            }
                            if (fieldRepeat.size > 1)
                                jsonArrayValues?.add(compJsonObj)
                            else
                                segFieldObj.add(segField.name.normalize(), compJsonObj)
                        }

                    }
                }
                if (!segConfig.children.isNullOrEmpty()) {//Do not change to isNotEmpty.
                    val childArray = JsonArray()
                    segJson.add("children", childArray)
                    segConfig.children.forEach { childSeg ->
                        processSegment("$seg[${segIndex + 1}]", childSeg.key, childSeg.value, childArray)
                    }
                }
            }
        }
    }



    fun JsonObject.addValueOrNull(value: String?, name: String) {
        if (!value.isNullOrEmpty())
            this.addProperty(name.normalize(), value)
        else this.add(name.normalize(),JsonNull.INSTANCE)
    }

    fun String.normalize(): String {
        return normalizeString(this)
    }

    fun normalizeString(str: String): String {
        val replaceableChars = mapOf(
            " " to "_",
            "-" to "_",
            "/" to "_",
            "." to "_",
            "&" to "_and_"
        )
        var rr1 = str.trim().lowercase()
        replaceableChars.forEach {
            rr1 = rr1.replace(it.key, it.value)
        }
        //remove duplicate underscores based on replacements above and remove all other unknown chars
        return rr1.replace("(_)\\1+".toRegex(), "_").replace("[^A-Z a-z 0-9 _\\.]".toRegex(), "")
    }
}
