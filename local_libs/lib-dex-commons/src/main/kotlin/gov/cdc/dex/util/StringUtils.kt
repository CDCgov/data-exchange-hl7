package gov.cdc.dex.util

import gov.cdc.dex.util.StringUtils.Companion.toHex
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

class StringUtils {

    companion object {
        //Public Functions:
        fun String.hashMD5(): String {
            return md5(this).toHex()
        }

        @JvmName("normalizeNullable")
        fun String?.normalize(): String? {
            return this?.normalize()
        }
        fun String.normalize(): String {
            return normalizeString(this)
        }


        private fun normalizeString(str: String): String {
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


        private fun md5(str: String): ByteArray = MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8))
        private fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }


    }

}
