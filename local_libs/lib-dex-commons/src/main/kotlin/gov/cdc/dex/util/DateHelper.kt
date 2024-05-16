package gov.cdc.dex.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

public object DateHelper {
    const val ISO_8601_24H_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    @JvmName("toIsoStringNullable")
    fun Date?.toIsoString(): String? {
        return this?.toIsoString()
    }

    fun Date.toIsoString(): String {
        val dateFormat: DateFormat = SimpleDateFormat(ISO_8601_24H_FULL_FORMAT)
        return dateFormat.format(this)
    }

    @JvmName("toIsoStringNullableOffsetDateTime")
    fun OffsetDateTime?.toIsoString(): String? {
        return this?.toIsoString()
    }
    fun OffsetDateTime.toIsoString(): String {
       // return Date.from(this.toInstant()).toIsoString() //converts to local time for some reason
        return this.format(DateTimeFormatter.ofPattern(ISO_8601_24H_FULL_FORMAT))
    }
}