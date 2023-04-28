package gov.cdc.dex.hl7

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField


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
            // all 9s and all 0s pattern has already been examined
            // by Structure Validator, so OK to pass through if present
            if (hl7Date == 9.toString() * hl7Date.length ||
                    hl7Date == 0.toString() * hl7Date.length) { return "OK" }
            // otherwise, validate date content
            val pattern = determineDatePattern(hl7Date)
            val formatter = DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT)
            return if (pattern.contains(HOUR)) {
                validateDateTime(hl7Date, formatter)
            } else if (pattern.contains(DAY)){
                validateDate(hl7Date, formatter)
            } else {
                validateDateParts(hl7Date, pattern)
            }

        }

        fun validateMMWRYear(mmwrYear: String): String {
            val formatter = getFormatterForIncompleteDate("uuuu")
            return try {
                val parsedYear = LocalDate.parse(mmwrYear, formatter)
                val currentYear = LocalDateTime.now().plusHours(24).toLocalDate()
                if (parsedYear <= currentYear) {
                    "OK"
                } else {
                    "Year cannot be greater than current year"
                }
            } catch (e : Exception) {
                "${e.message}"
            }

        }

        fun validateMMWRWeek(mmwrWeek: String): String {
            return try {
                val parsedWeek = mmwrWeek.toInt()
                if (parsedWeek in 1..52) {
                    "OK"
                } else {
                    "Week must be in the range 1 to 52"
                }
            } catch (e: Exception) {
                "'$mmwrWeek' is not a valid week"
            }
        }

        private fun validateDateParts(dateString: String, pattern: String) : String {
            if (dateString.length % 2 != 0) {
                return "Unable to parse date of length ${dateString.length}"
            }
           // since month and day are optional, but LocalDate requires them,
           // this will insert default values for month and day if needed
            return validateDate(dateString, getFormatterForIncompleteDate(pattern))
        }

        private fun getFormatterForIncompleteDate(pattern: String) : DateTimeFormatter {
            return DateTimeFormatterBuilder()
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .toFormatter()

        }
        private fun validateDate(dateString: String, formatter: DateTimeFormatter): String {
            return try {
                LocalDate.parse(dateString, formatter)
                "OK"
            } catch (e: Exception) {
                "${e.message}"
            }
        }

        private fun validateDateTime(dateTimeString: String, formatter: DateTimeFormatter): String {
            return try {
                LocalDateTime.parse(dateTimeString, formatter)
                "OK"
            } catch (e: Exception) {
                "${e.message}"
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
