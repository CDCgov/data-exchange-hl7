package gov.cdc.dex.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class StringUtilsTest {

    @Test
    fun normalizeString() {
        val newName = StringUtils.normalizeString("Fn 2.0-V1.0  & (Typhi/S)")
        println(newName)
        assertTrue(newName == "fn_2.0_v1.0_and_typhi_s")

    }
}