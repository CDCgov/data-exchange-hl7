
import org.junit.jupiter.api.Test

class TestLineCleanup {

    @Test
    fun testLineTrim() {
        val line = "   abc   "
        val lineClean = line.trim().let { if (it.startsWith("a")) it.substring(1) else it }
        println(lineClean)
        assert(lineClean == "bc")
    }

    private fun getValueOrDefault(
        metaDataMap: Map<String, String?>,
        keysToTry: List<String>,
        defaultReturnValue: String = "UNKNOWN"
    ): String {
        keysToTry.forEach { if (!metaDataMap[it].isNullOrEmpty()) return metaDataMap[it]!! }
        return defaultReturnValue
    }

    @Test
    fun testGetValueOrDefault() {
        val metaDataMap = mapOf("meta_ext_uploadid" to null, "tus_tguid" to "1234567", "upload_id" to "9876543")
        val value = getValueOrDefault(metaDataMap, listOf("upload_id", "tus_tguid", "meta_ext_uploadid"))
        assert(value == "9876543")

    }

}