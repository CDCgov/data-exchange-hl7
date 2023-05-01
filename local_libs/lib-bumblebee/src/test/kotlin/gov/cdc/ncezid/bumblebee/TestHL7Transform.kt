package gov.cdc.ncezid.bumblebee



import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
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
}