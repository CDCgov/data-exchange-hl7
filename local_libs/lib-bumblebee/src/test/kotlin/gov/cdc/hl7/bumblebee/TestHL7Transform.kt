package gov.cdc.hl7.bumblebee


import com.google.gson.*
import gov.cdc.hl7.HL7StaticParser

import org.junit.jupiter.api.Test
import scala.Option


class TestHL7Transform {

    @Test
    fun testBuildHL7Tree() {

        val fullHl7 = JsonObject()

        val msh = JsonObject()
        msh.addProperty("file_separator", "|")
        msh.addProperty("encoding_characters", "^~\\&")

        val msh_3 = JsonObject()
        msh_3.addProperty("namespace_id", "ttt")
        msh_3.addProperty("universal_id", "2.16.840.1.114222.4.3.3.9")
        msh_3.addProperty("universal_id_type", "ISO")

        msh.add("sending_application", msh_3)

        val msh21Array = JsonArray()

        val msh21_1 = JsonObject()
        msh21_1.addProperty("entity_identifier", "NND_ORU_v2.0")
        msh21_1.addProperty("namespace_id", "PHINProfileID")
        msh21_1.addProperty("universal_id", "2.16.840.1.114222.4.10.3")
        msh21_1.addProperty("universal_id_type", "ISO")

        val msh21_2 = JsonObject()
        msh21_2.addProperty("entity_identifier", "TB_Case_Map_v2.0")
        msh21_2.addProperty("namespace_id", "PHINProfileID")
        msh21_2.addProperty("universal_id", "2.16.840.1.114222.4.10.4")
        msh21_2.addProperty("universal_id_type", "ISO")

        msh21Array.add(msh21_1)
        msh21Array.add(msh21_2)

        msh.add("message_profile_identifier", msh21Array)

        val pid = JsonObject()

        pid.addProperty("set_id", 1)
        pid.add("patient_id", JsonNull.INSTANCE)

        val pid_3CX = JsonObject()
        pid_3CX.addProperty("id_number", "PBQEEBEWJDYOED")
        pid_3CX.add("check_digit", JsonNull.INSTANCE)
        pid_3CX.add("check_digit_scheme", JsonNull.INSTANCE)

        val pid_3_4 = JsonObject()
        pid_3_4.add("namespace_id", JsonNull.INSTANCE)
        pid_3_4.addProperty("universal_id", "Maven EDSS")
        pid_3_4.addProperty("universal_id_type", "ISO")

        pid_3CX.add("assigning_authority", pid_3_4)
        pid_3CX.add("identifier_type_code", JsonNull.INSTANCE)
        pid_3CX.add("assigning_facility", JsonNull.INSTANCE)
        pid_3CX.add("effective_date", JsonNull.INSTANCE)
        pid_3CX.add("expiration_date", JsonNull.INSTANCE)
        pid_3CX.add("assigning_jurisdiction", JsonNull.INSTANCE)
        pid_3CX.add("assigning_agency_or_department", JsonNull.INSTANCE)

        pid.add("patient_identifier_list", pid_3CX)

        val mshChildren = JsonObject()
        mshChildren.add("PID", pid)

        val sft = JsonObject()

        mshChildren.add("SFT", sft)

        val obr = JsonObject()
        obr.addProperty("set_id", "1")
        obr.add("place_order_number", JsonNull.INSTANCE)

        val obr_3 = JsonObject() //A0803798^^Maven EDSS^ISO
        obr_3.addProperty("entity_identifier", "A0803798")
        obr_3.add("namespace_id", JsonNull.INSTANCE)
        obr_3.addProperty("universal_id", "Maven EDSS")
        obr_3.addProperty("universal_id_type", "ISO")

        obr.add("filler_order_number", obr_3)

        val obrChildren = JsonObject()

        val obx =
            JsonObject() //OBX|1|CWE|DEM114^Birth Sex^2.16.840.1.114222.4.5.232|1|U^Unknown^2.16.840.1.113883.12.1||||||F
        obx.addProperty("set_id", "1")
        obx.addProperty("value_type", "CWE")

        val obx3 = JsonObject() //DEM114^Birth Sex^2.16.840.1.114222.4.5.232
        obx3.addProperty("identifier", "DEM114")
        obx3.addProperty("text", "Birth Sex")
        obx3.addProperty("name_of_coding_system", "2.16.840.1.114222.4.5.232")

        obx.add("observation_identifier", obx3)

        val obx5 = JsonObject() //U^Unknown^2.16.840.1.113883.12.1
        obx5.addProperty("identifier", "U")
        obx5.addProperty("text", "Unknown")
        obx5.addProperty("name_of_coding_system", "2.16.840.1.113883.12.1")

        obx.add("observation_value", obx5)
        obx.addProperty("observation_result_status", "F")

        obrChildren.add("OBX", obx)
        obr.add("children", obrChildren)

        mshChildren.add("OBR", obr)
        msh.add("children", mshChildren)


        fullHl7.add("MSH", msh)

        val json = GsonBuilder().serializeNulls().create().toJson(fullHl7)
        println(json)
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

    @Test
    fun testCardinality() {
        val cardReg = "\\[(\\d)\\.\\.([\\d|*])".toRegex()

        val cardTest = "[abc]"

            val start = try {
                cardTest.substring(1, cardTest.indexOf(".."))
                        } catch (e: StringIndexOutOfBoundsException) { "UNK" }
        val end = try {
            cardTest.substring(cardTest.indexOf("..")+2, cardTest.length -1)
                }catch (e: StringIndexOutOfBoundsException) {
                "UNK" }
//        val m = cardReg.find("[0..2]")
//        m.iterator().forEach {
//            println(it.value)
//        }
        println("Start: $start\nend: $end")

    }
    @Test
    fun testHL7Transformer() {
//        val message = this::class.java.getResource("/COVID.txt").readText()
        val message = this::class.java.getResource("/WI v231.txt").readText()
        val gson = GsonBuilder().serializeNulls().create()


        val xformer = HL7JsonTransformer.getTransformerWithResource(message, "PhinGuideProfile.json")
        val fullHL7 = xformer.transformMessage()
        println(gson.toJson(fullHL7))
    }

    @Test
    fun loadProfiles() {
        val message = this::class.java.getResource("/COVID.txt").readText()

        val content = this::class.java.getResource("/PhinGuideProfile.json").readText()

        val gson = GsonBuilder().create()

        val profile: Profile = gson.fromJson(content, Profile::class.java)

        val defaultFieldsProfileContent = this::class.java.getResource("/DefaultFieldsProfile.json").readText()

        //val defaultFieldsProfileGson = Gson()

        val fieldProfile: Profile = gson.fromJson(defaultFieldsProfileContent, Profile::class.java)
        val fullHl7 = JsonObject()


        val lines = message.split("\n")
        val childrenArray = JsonArray()
        lines.forEach {
            if (it.startsWith("MSH|")) {
                val msh = JsonObject()
                val msg = it
                val encodeChars = msg.split("|")[1]

                profile.getSegmentField("MSH")?.forEach {

                    if (it.fieldNumber == 1) {
                        msh.addProperty(normalizeString(it.name), "|")
                    }
                    if (it.fieldNumber == 2) {
                        msh.addProperty(normalizeString(it.name), encodeChars)
                    }
                    if (it.fieldNumber >= 3) {
                        val msh_4 = JsonObject()
                        val field = it.fieldNumber;

                        if (it.dataType == "TS" || it.dataType == "ST" || it.dataType == "NM") {
                            val value = HL7StaticParser.getFirstValue(msg, "MSH-" + field + "[1]")
                            addProperty(value, msh, it)
                        } else {
                            if (it.cardinality == "[2..3]") {
                                val jArray = JsonArray()

                                messageProfileIdentifier(msg, field, fieldProfile, it, jArray)

                                msh.add(normalizeString(it.name), jArray)
                            } else {
                                fieldProfile.getSegmentField(it.dataType)?.forEach {
                                    val value = HL7StaticParser.getFirstValue(
                                        msg,
                                        "MSH-" + field + "[1]." + it.fieldNumber + ""
                                    )
                                    addProperty(value, msh, it)
                                }
                                msh.add(normalizeString(it.name), msh_4)
                            }
                        }
                    }
                }
                fullHl7.add("MSH", msh)
            } else {

                val msh = JsonObject()
                val msg = it
                val segmentField = it.split("|")[0]

                println(segmentField)
                if (it.startsWith(segmentField + "|")) {
                    val pidObject = JsonObject()
                    profile.getSegmentField(segmentField)?.forEach {

                        val msh_4 = JsonObject()
                        val field = it.fieldNumber;

                        if (it.dataType == "SI" || it.dataType == "IS" || it.dataType == "ID" ||
                            it.dataType == "TS" || it.dataType == "ST" || it.dataType == "NM"
                        ) {
                            val value = HL7StaticParser.getFirstValue(msg, segmentField + "-" + field + "[1]")

                            addProperty(value, msh, it)

                        } else if (it.dataType == "CX" || it.dataType == "CE"
                            || it.dataType == "XPN" || it.dataType == "XAD"
                        ) {
                            fieldProfile.getSegmentField(it.dataType)?.forEach {
                                if (it.dataType == "HD" && it.cardinality == "[1..1]") {
                                    var childObject = JsonObject()
                                    fieldProfile.getSegmentField(it.dataType)?.forEach {
                                        val value = HL7StaticParser.getFirstValue(
                                            msg,
                                            segmentField + "-" + field + "[1]." + it.fieldNumber + ""
                                        )
                                        addProperty(value, msh, it)

                                    }
                                    msh_4.add(normalizeString(it.name), childObject)
                                } else {
                                    val value = HL7StaticParser.getFirstValue(
                                        msg,
                                        segmentField + "-" + field + "[1]." + it.fieldNumber + ""
                                    )
                                    addProperty(value, msh, it)

                                }
                            }
                            msh.add(normalizeString(it.name), msh_4)
                        }
                        pidObject.add(segmentField, msh)
                    }
                    childrenArray.add(pidObject)
                }
                println("childrenarray")
                println(childrenArray)
                fullHl7.add("Children", childrenArray)
            }
        }
        val json = GsonBuilder().serializeNulls().create().toJson(fullHl7)
        println("RESULT!!!!")
        println(json)
    }



private fun messageProfileIdentifier(msg: String,field: Int, defaultProfile: Profile, it: HL7SegmentField, jArray: JsonArray) {
    val value = HL7StaticParser.getValue( msg,"MSH-" + field)
    val valueFlat = value.get().flatten()
    valueFlat.forEachIndexed { idx, itt ->
        val msh21 = JsonObject()
        val compParts = itt.split("^")
        defaultProfile.getSegmentField(it.dataType)?.forEachIndexed { cidx, comp ->

            val key = comp.name
            if (key != null) {
                msh21.addProperty(normalizeString(key), compParts[cidx])
            } else {
                msh21.add(normalizeString(key), JsonNull.INSTANCE)
            }
        }
        jArray.add(msh21)
    }
}

private fun addProperty(
    value: Option<String>,
    msh: JsonObject,
    it: HL7SegmentField
): JsonObject {
    if (value.isDefined) {
        msh.addProperty(normalizeString(it.name), value.get())
    } else {
        msh.add(normalizeString(it.name), JsonNull.INSTANCE)
    }
    return msh
}

@Test
fun loadFieldDef() {
    val content = this::class.java.getResource("/DefaultFieldsProfile.json").readText()

    val gson = Gson()

    val profile: Profile = gson.fromJson(content, Profile::class.java)
    println(profile)
    println((profile.getSegmentField("HD")?.get(0))?.name)
    println((profile.getSegmentField("HD")?.get(1))?.name)
    println((profile.getSegmentField("HD")?.get(2))?.name)
    println(profile.segmentFields.keys);
}
}