package gov.cdc.dex.util

import gov.cdc.dex.util.StringUtils.Companion.hashMD5
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.BufferedReader
import java.io.InputStreamReader

internal class StringUtilsTest {

    @Test
    fun normalizeString() {
        val newName = StringUtils.normalizeString("Fn 2.0-V1.0  & (Typhi/S)")
        println(newName)
        assertTrue(newName == "fn_2_0_v1_0_and_typhi_s")
    }
    
    @Test
    fun normalizeAllMMGNames() {
        val mmgKeys = listOf("mmg:mumps", "mmg:measles", "mmg:abcs_h._influenzae", "mmg:lyme_disease", "mmg:abcs_n._meningitidis", "mmg:varicellacasenationalnotificationmap_v1_0", "mmg:pertussis", "mmg:congenital_syphilis", "mmg:abcs_group_a_streptococcus", "mmg:varicella_message_mapping_guide_v2_01", "mmg:std_1.2", "mmg:babesiosis_v1.1", "mmg:rubella", "mmg:arboviral_human_case_message_mapping_guide", "mmg:trichinellosis", "mmg:covid_19", "mmg:z_testing", "mmg:tbrd", "mmg:hai_mdro_candida_auris", "mmg:hai_mdro_cp_cre", "mmg:congenital_rubella_syndrome", "mmg:covid_19_v1.1", "mmg:hepatitis_core_v1.0.1_not_review", "mmg:covid_19_v1.2", "mmg:varicella", "mmg:malaria", "mmg:carbon_monoxide_poisoning", "mmg:tuberculosis_and_ltbi_v3.0.3", "mmg:babesiosis", "mmg:generic_summary_case_notification_mmg_v1_0", "mmg:congenital_syphilis_1.1", "mmg:abcs_group_b_streptococcus", "mmg:generic_case_notification_message_mapping_guide_v1_0", "mmg:tuberculosis_message_mapping_guide_v2_03", "mmg:abcs_invasive_pneumococcal_disease", "mmg:ribd_psittacosis", "mmg:arboviral_v1_3_2_mmg_20210721", "mmg:std")

        val normalizedKeys = mmgKeys.map { StringUtils.normalizeString(it.substring(3))}
        normalizedKeys.forEach { println(it)}
    }

    @Test
    fun hashString() {
        val test = this::class.java.getResource("/KY_Hepatitis Round 2_TM1.txt").readText().hashMD5()
        println(test)
//896b4684f7e7246124050113836b75cf

        val currentLinesArr = arrayListOf<String>()

        val testFileIS = this::class.java.getResource("/KY_Hepatitis Round 2_TM1.txt").openStream()
        BufferedReader(InputStreamReader(testFileIS)).use { br ->
            br.forEachLine { line ->
                currentLinesArr.add(line)
            }
        }
        val newMsg = currentLinesArr.joinToString ( "\n" )
//        println(newMsg)
        println(newMsg.hashMD5())
    }

    @Test
    fun testShortName() {
        val name = "Vaccine Related Repeating Group Section About Vaccinations and Other Vaccine Related Stuff"
        val shortName = StringUtils.getNormalizedShortName(name, 30)
        println(shortName)
        assert(shortName.length <= 30)
        val name2 = StringUtils.getNormalizedShortName("Clinical Manifestations Of Disease Repeating Group", 30)
        println(name2)
        assert(name2.length <= 30)
        val name3 = StringUtils.getNormalizedShortName("Clinical Manifestations of Disease", 30)
        println(name3)
        assert(name3 != name2)
        val name4 = StringUtils.getNormalizedShortName("Tick Bite Repeating Group", 30)
        println(name4)
        val name5 = StringUtils.getNormalizedShortName(("Tick Bite Repeating Group"))
        println(name5)
        assert(name4 == name5)
        val name6 = StringUtils.getNormalizedShortName("Tick Bite")
        println(name6)
        assert (name6 != name5)
        val name7 = StringUtils.getNormalizedShortName("Tick Bite Repeating Group", 2)
        println(name7)
    }
}