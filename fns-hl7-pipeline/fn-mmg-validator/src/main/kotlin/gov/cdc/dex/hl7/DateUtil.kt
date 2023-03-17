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

        private val PATTERNS = hashMapOf<Int,String>(
            4 to "$YEAR$OPTIONAL_ZONE",
            6 to "$YEAR$MONTH$OPTIONAL_ZONE",
            8 to "$YEAR$MONTH$DAY$OPTIONAL_ZONE",
            12 to "$YEAR$MONTH$DAY$HOUR$MINUTE$OPTIONAL_ZONE",
            14 to "$YEAR$MONTH$DAY$HOUR$MINUTE$SECOND$OPTIONAL_ZONE",
            16 to "$YEAR$MONTH$DAY$HOUR$MINUTE$SECOND.$MILLISECOND$OPTIONAL_ZONE",
            17 to "$YEAR$MONTH$DAY$HOUR$MINUTE$SECOND.${MILLISECOND * 2}$OPTIONAL_ZONE",
            18 to "$YEAR$MONTH$DAY$HOUR$MINUTE$SECOND.${MILLISECOND * 3}$OPTIONAL_ZONE",
            19 to "$YEAR$MONTH$DAY$HOUR$MINUTE$SECOND.${MILLISECOND * 4}$OPTIONAL_ZONE"
        )

        fun validateHL7Date(hl7Date: String): String {
            val pattern = determineDatePattern(hl7Date)
            return if (pattern.isNotEmpty()) {
                val formatter = DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT)
                if (pattern.contains(HOUR)) {
                    validateDateTime(hl7Date, formatter)
                } else {
                    validateDate(hl7Date, formatter)
                }
            } else {
                "Error: Invalid date format"
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
            return PATTERNS[len] ?: ""
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
