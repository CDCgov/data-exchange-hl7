import gov.cdc.dex.hl7.model.MmgReport
import gov.cdc.dex.hl7.model.ValidationIssue
import gov.cdc.dex.hl7.model.ValidationIssueCategoryType
import org.testng.annotations.Test
import kotlin.test.DefaultAsserter.assertTrue

class TestCardinaltyMatch {
    operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

    fun getCardinality(cardinality: String)  {
        val card1Re = """\d+|\*""".toRegex()
        val cards = card1Re.findAll(cardinality)
        val card1 = cards.elementAt(0).value
        val card2 = cards.elementAt(1).value
        println("checking: $card1 to $card2")

        when (card1) {
            "0" ->  println("\tOptional")
            else -> println("\tRequired at least $card1")
        }
        when (card2) {
            "1" -> println("\t\tMax One")
            "*" -> println ("\t\tUnbounded")
            else -> println("\t\tBounded. max $card2")
        }
    }
    @Test
    fun testCardinaltiy() {
        getCardinality("[0..1]")
        getCardinality("[0..*]")
        getCardinality("[1..1]")
        getCardinality("[1..*]")
        getCardinality("[2..32]")
    }

    @Test
    fun testIssueClassification() {
        val errorClass = ValidationIssueCategoryType.ERROR
        assert(errorClass.toString() == "ERROR")
        assert(errorClass == ValidationIssueCategoryType.ERROR)
        assert(errorClass === ValidationIssueCategoryType.ERROR)
    }

    @Test
    fun testMMGReport() {
        val entries = listOf<ValidationIssue>()
        val errorCount = entries.count { it.classification == ValidationIssueCategoryType.ERROR}
        val warnCount = entries.count { it.classification == ValidationIssueCategoryType.WARNING}
        val report = MmgReport(errorCount, warnCount, listOf())

        println(report)
        println(report.status)
    }
}