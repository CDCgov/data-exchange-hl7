
import org.junit.jupiter.api.Test

class TestLineCleanup {

    @Test
    fun testLineTrim() {
        val line = "   abc   "
        val lineClean = line.trim().let { if ( it.startsWith("a") )  it.substring(1)  else it}
        println(lineClean)
        assert(lineClean == "bc")
    }
}