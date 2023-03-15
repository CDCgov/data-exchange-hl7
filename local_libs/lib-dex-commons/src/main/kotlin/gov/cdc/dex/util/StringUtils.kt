package gov.cdc.dex.util

import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

class StringUtils {
    companion object {
        fun normalizeString(str: String): String {
            val replaceableChars = mapOf(
                " " to "_",
                "-" to "_",
                "/" to "_",
                "." to "_",
                "&" to "_and_")

            var rr1 = str.trim().lowercase()
            replaceableChars.forEach {
                rr1 = rr1.replace(it.key, it.value)
            }
            //remove duplicate underscores based on replacements above and remove all other unknown chars
            return rr1.replace("(_)\\1+".toRegex(), "_" ).replace("[^A-Z a-z 0-9 _\\.]".toRegex(), "")
        }

        fun String.normalize(): String {
            return normalizeString(this)
        }
        private fun md5(str: String): ByteArray = MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8))
        private fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }
        fun String.hashMD5(): String {
            return md5(this).toHex()
        }

        fun getNormalizedShortName(name: String, maxLength: Int = 0) : String {
            var shortName = name.normalize()
            if (shortName.contains("_repeating_group")) {
                shortName = truncateAndReplaceRepeatingGroupWithRg(shortName, maxLength)
            } else {
                if (maxLength > 0 && shortName.length > maxLength) {
                    shortName = shortName.substring(0, maxLength)
                }
                if (shortName.endsWith("_")) {
                    shortName = shortName.substringBeforeLast("_")
                }
            }
            return shortName
        }

        private fun truncateAndReplaceRepeatingGroupWithRg(name: String, maxLength: Int) : String {
            var newName = name.substringBefore("_repeating_group")
            // still might be too long, so shorten and account for length of "_rg"
            if (maxLength > 0 && newName.length > maxLength) newName = if (maxLength > 3) {
                newName.substring(0, maxLength - 3)
            } else {
                newName.substring(0, maxLength)
            }
            newName += if (newName.endsWith("_")) {
                "rg"
            } else {
                "_rg"
            }
            return newName
        }

    }

}