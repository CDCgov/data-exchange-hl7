package gov.cdc.dex.util

import gov.cdc.dex.util.DateHelper.toIsoString
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

internal class DateHelperTest {

    @Test
    fun testDateToISO() {
        val now = Date()
        val iso = now.toIsoString()
        println(iso)
    }

    @Test
    fun testOffsetDateTimeToISO() {
        val submissionDate:OffsetDateTime = OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
        val iso = submissionDate.toIsoString()
        println(iso)
    }
}