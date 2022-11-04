package gov.cdc.dex.util

class StringUtils {
    companion object {
        fun normalizeString(str: String): String {
            val replaceableChars = listOf(" ", "-", "/", "&")

            var rr1 = str.trim().lowercase()
            replaceableChars.forEach {
                rr1 = rr1.replace(it, "_")
            }
            return rr1
        }
    }
}