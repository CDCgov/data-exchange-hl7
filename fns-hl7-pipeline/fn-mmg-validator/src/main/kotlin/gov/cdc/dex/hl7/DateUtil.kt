package gov.cdc.dex.hl7

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

class DateUtil {
    companion object {
        const val YEAR = "uuuu"
        const val MONTH = "MM"
        const val DAY = "dd"
        const val HOUR = "HH"
        const val MINUTE = "mm"
        const val SECOND = "ss"
        const val MILLISECOND = "S"
        const val OPTIONAL_ZONE = "[Z]"

        fun validateHL7Date(hl7Date: String): String {
            val pattern = determineDatePattern(hl7Date)
            val formatter = DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT)
            return if (pattern.contains(HOUR)) {
                validateDateTime(hl7Date, formatter)
            } else {
                validateDate(hl7Date, formatter)
            }

        }

   /*     fun validateMMWRYear(mmwrYear: String): String {

        }

        fun validateMMWRDay(mmwrDay: String): String {

        }
*/
        private fun validateDate(dateString: String, formatter: DateTimeFormatter): String {
            return try {
                val localDate = LocalDate.parse(dateString, formatter)
                "OK"
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }

        private fun validateDateTime(dateTimeString: String, formatter: DateTimeFormatter): String {
            return try {
                val localDateTime = LocalDateTime.parse(dateTimeString, formatter)
                "OK"
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }


        private fun determineDatePattern(dateString: String): String {
            val delimiter = if (dateString.contains("+")) {
                "+"
            } else if (dateString.contains("-")) {
                "-"
            } else {
                ""
            }
            val len = if (delimiter.isNotEmpty()) {
                dateString.substringBefore(delimiter).length
            } else {
                dateString.length
            }
            return buildPattern(len)
        }

        private fun buildPattern(length: Int) : String {
            val builder = StringBuilder()
            if (length >= 4) builder.append(YEAR)
            if (length >= 6) builder.append(MONTH)
            if (length >= 8) builder.append(DAY)
            if (length >= 10) builder.append(HOUR)
            if (length >= 12) builder.append(MINUTE)
            if (length >= 14) builder.append(SECOND)
            if (length in 16..19) builder.append(".${MILLISECOND * (length - 15)}")
            builder.append(OPTIONAL_ZONE)
            return builder.toString()
        }
    }

}

private operator fun String.times(i: Int): String {
    val builder = StringBuilder()
    for (n in 1..i) {
        builder.append(this)
    }
    return builder.toString()
}
