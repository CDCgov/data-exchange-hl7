package gov.cdc.hl7.bumblebee



import com.google.gson.*
import com.google.gson.reflect.TypeToken
import gov.cdc.hl7.HL7StaticParser

import org.junit.jupiter.api.Test



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
        msh21_1.addProperty("entity_identifier","NND_ORU_v2.0")
        msh21_1.addProperty("namespace_id", "PHINProfileID")
        msh21_1.addProperty("universal_id", "2.16.840.1.114222.4.10.3")
        msh21_1.addProperty("universal_id_type", "ISO")

        val msh21_2 = JsonObject()
        msh21_2.addProperty("entity_identifier","TB_Case_Map_v2.0")
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
        pid_3CX.addProperty("id_number","PBQEEBEWJDYOED" )
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
        obr_3.addProperty("entity_identifier","A0803798")
        obr_3.add("namespace_id", JsonNull.INSTANCE)
        obr_3.addProperty("universal_id", "Maven EDSS")
        obr_3.addProperty("universal_id_type", "ISO")

        obr.add("filler_order_number", obr_3)

        val obrChildren = JsonObject()

        val obx = JsonObject() //OBX|1|CWE|DEM114^Birth Sex^2.16.840.1.114222.4.5.232|1|U^Unknown^2.16.840.1.113883.12.1||||||F
        obx.addProperty("set_id", "1")
        obx.addProperty("value_type", "CWE")

        val obx3 = JsonObject() //DEM114^Birth Sex^2.16.840.1.114222.4.5.232
        obx3.addProperty("identifier", "DEM114")
        obx3.addProperty("text", "Birth Sex")
        obx3.addProperty("name_of_coding_system", "2.16.840.1.114222.4.5.232" )

        obx.add("observation_identifier", obx3)

        val obx5 = JsonObject() //U^Unknown^2.16.840.1.113883.12.1
        obx5.addProperty("identifier", "U")
        obx5.addProperty("text", "Unknown")
        obx5.addProperty("name_of_coding_system", "2.16.840.1.113883.12.1" )

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
           "&" to "_and_")
        var rr1 = str.trim().lowercase()
        replaceableChars.forEach {
            rr1 = rr1.replace(it.key, it.value)
        }
        //remove duplicate underscores based on replacements above and remove all other unknown chars
        return rr1.replace("(_)\\1+".toRegex(), "_" ).replace("[^A-Z a-z 0-9 _\\.]".toRegex(), "")
    }
    @Test
    fun loadProfiles() {
        val message = this::class.java.getResource("/COVID.txt").readText()

        val content = this::class.java.getResource("/PhinGuideProfile.json").readText()

        val gson = GsonBuilder()
                //.registerTypeAdapter(Map::class.java, HashMap::class.java)
                .create()

        val profile: Profile = gson.fromJson(content, Profile::class.java)

        val defaultFieldsProfileContent = this::class.java.getResource("/DefaultFieldsProfile.json").readText()

        val defaultFieldsProfileGson = Gson()

        val defaultProfile: Profile = defaultFieldsProfileGson.fromJson(defaultFieldsProfileContent, Profile::class.java)
        println("defaultProfile")
        println(defaultProfile)
        /*println((defaultProfile.getSegmentField("HD")?.get(0))?.name)
        println((defaultProfile.getSegmentField("HD")?.get(1))?.name)
        println((defaultProfile.getSegmentField("HD")?.get(2))?.name)*/

        println("Profile")
        println(profile)
        println("\n\nMSH Fields")
        val fullHl7 = JsonObject()

        val lines =message.split("\n")
        lines.forEach {
            if (it.startsWith("MSH|")) {
                val msh = JsonObject()

                val msg = it
                println("Hello")
                val encodeChars = msg.split("|")[1]
               // val sendApplication = msg.split("|")[2]
                println(it.split("|")[1])
                println(HL7StaticParser.getFirstValue(msg, "MSH-3[1]."+1+"").get())
                println(HL7StaticParser.getFirstValue(msg, "MSH-3[1].2").get())
                println(HL7StaticParser.getFirstValue(msg, "MSH-3[1].3").get())
                println("GoodBye")

                profile.getSegmentField("MSH")?.forEach {
                    println(it)
                    println(it.name)
                    println(it.dataType)
                    if(it.fieldNumber==1) {
                        msh.addProperty(normalizeString(it.name), "|")
                    }
                    if(it.fieldNumber==2){
                        msh.addProperty(normalizeString(it.name), encodeChars)
                    }
                    if(it.fieldNumber>=3){
                        val msh_4 = JsonObject()
                        val field = it.fieldNumber;

                        if(it.dataType=="TS" || it.dataType=="ST" || it.dataType=="NM"){
                            val value = HL7StaticParser.getFirstValue(msg, "MSH-"+field+"[1]")
                            if(value.isDefined){
                                msh.addProperty(normalizeString(it.name), value.get())
                            }else{
                                msh.add(normalizeString(it.name), JsonNull.INSTANCE)
                            }
                        }
                        else{
                            if(it.cardinality=="[2..3]"){
                                val jArray = JsonArray()
                                val value = HL7StaticParser.getValue(
                                    msg,
                                    "MSH-" + field
                                )
                                val valueFlat = value.get().flatten()
                                valueFlat.forEachIndexed { idx, itt ->
                                    val msh21 = JsonObject()
                                    val compParts = itt.split("^")
                                    defaultProfile.getSegmentField(it.dataType)?.forEachIndexed { cidx, comp ->

                                        val key = comp.name
                                        if(key!=null) {
                                            msh21.addProperty(normalizeString(key), compParts[cidx])
                                        }else{
                                            msh21.add(normalizeString(key), JsonNull.INSTANCE)
                                        }
                                    }
                                    jArray.add(msh21)
                                }
                                msh.add(normalizeString(it.name), jArray)
                            }else {
                                defaultProfile.getSegmentField(it.dataType)?.forEach {
                                    val value = HL7StaticParser.getFirstValue(
                                        msg,
                                        "MSH-" + field + "[1]." + it.fieldNumber + ""
                                    )
                                    if (value.isDefined) {
                                        msh_4.addProperty(normalizeString(it.name), value.get())
                                    } else {
                                        msh_4.add(normalizeString(it.name), JsonNull.INSTANCE)
                                    }
                                }
                                msh.add(normalizeString(it.name), msh_4)
                            }
                        }
                    }
                }
                fullHl7.add("MSH", msh)
            }else {
                val childrenArray = JsonArray()
                val msh = JsonObject()
                val msg = it
                if(it.startsWith("PID|")) {
                    val pidObject = JsonObject()
                    profile.getSegmentField("PID")?.forEach {
                        println("..............PID............")
                        println(it)
                        println(it.name)
                        println(it.dataType)
                        val msh_4 = JsonObject()
                        val field = it.fieldNumber;

                        if (it.dataType == "SI" || it.dataType == "IS" || it.dataType == "ID" ||
                            it.dataType == "TS" || it.dataType == "ST" || it.dataType == "NM"
                        ) {
                            println("PID-1" + HL7StaticParser.getFirstValue(msg, "PID-1").get())
                            println("PID-1[1]" + HL7StaticParser.getFirstValue(msg, "PID-1[1]").get())


                            val value = HL7StaticParser.getFirstValue(msg, "PID-" + field + "[1]")
                            if (value.isDefined) {
                                msh.addProperty(normalizeString(it.name), value.get())
                            } else {
                                msh.add(normalizeString(it.name), JsonNull.INSTANCE)
                            }
                        }else if(it.dataType == "CX" || it.dataType == "CE"
                            || it.dataType == "XPN" || it.dataType == "XAD"){
                            defaultProfile.getSegmentField(it.dataType)?.forEach {
                                if(it.dataType=="HD" && it.cardinality=="[1..1]"){
                                    var childObject = JsonObject()
                                    defaultProfile.getSegmentField(it.dataType)?.forEach {
                                        val value = HL7StaticParser.getFirstValue(
                                            msg,
                                            "PID-" + field + "[1]." + it.fieldNumber + ""
                                        )
                                        if (value.isDefined) {
                                            childObject.addProperty(normalizeString(it.name), value.get())
                                        } else {
                                            childObject.add(normalizeString(it.name), JsonNull.INSTANCE)
                                        }
                                    }
                                    msh_4.add(normalizeString(it.name), childObject)
                                }else {
                                    val value = HL7StaticParser.getFirstValue(
                                        msg,
                                        "PID-" + field + "[1]." + it.fieldNumber + ""
                                    )
                                    if (value.isDefined) {
                                        msh_4.addProperty(normalizeString(it.name), value.get())
                                    } else {
                                        msh_4.add(normalizeString(it.name), JsonNull.INSTANCE)
                                    }
                                }
                            }
                            msh.add(normalizeString(it.name), msh_4)
                        }

                        pidObject.add("PID", msh)
                    }
                    childrenArray.add(pidObject)
                    fullHl7.add("Children", childrenArray)
                }
println("childrenarray")
                println(childrenArray)
            }
        }
        val json = GsonBuilder().serializeNulls().create().toJson(fullHl7)
        println("Updated Schema")
        println(json)
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