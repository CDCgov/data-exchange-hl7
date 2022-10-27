package gov.cdc.dex.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

public object DateHelper {
    private const val ISO_8601_24H_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"

    @JvmName("toIsoStringNullable")
    fun Date?.toIsoString(): String? {
        return this?.toIsoString()
    }

    fun Date.toIsoString(): String {
        val dateFormat: DateFormat = SimpleDateFormat(ISO_8601_24H_FULL_FORMAT)
        return dateFormat.format(this)
    }
}