import gov.cdc.dex.hl7.model.ValidationIssueCategoryType
import org.junit.jupiter.api.Test

class TestCardinalityMatch {
    operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

    private fun getCardinality(cardinality: String)  {
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
    fun testCardinality() {
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


}