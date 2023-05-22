package gov.cdc.hl7.bumblebee

class StringUtils {
    companion object {
        //Extension method to "normalize" a string that will be used to be a name of some resource or artifacts, like
        //table names, column names or json attributes.
        fun String.normalize(): String {
            return normalizeString(this)
        }

        fun normalizeString(str: String): String {
            val replaceableChars = mapOf(
                " " to "_",
                "-" to "_",
                "/" to "_",
                "." to "_",
                "&" to "_and_"
            )
            var rr1 = str.trim().lowercase()
            replaceableChars.forEach {
                rr1 = rr1.replace(it.key, it.value)
            }
            //remove duplicate underscores based on replacements above and remove all other unknown chars
            return rr1.replace("(_)\\1+".toRegex(), "_").replace("[^A-Z a-z 0-9 _\\.]".toRegex(), "")
        }
    }
}