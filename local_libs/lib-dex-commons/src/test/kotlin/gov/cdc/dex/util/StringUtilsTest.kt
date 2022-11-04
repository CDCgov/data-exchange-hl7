package gov.cdc.dex.util

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class StringUtilsTest {

    @Test
    fun normalizeString() {
        val newName = StringUtils.normalizeString("Fn 2.0-V1.0 & Typhi/S")
        println(newName)

    }
}