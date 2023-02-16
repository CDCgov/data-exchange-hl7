import gov.cdc.hl7.HL7HierarchyParser
import gov.cdc.hl7.model.HL7Hierarchy
import org.junit.jupiter.api.Test

class TestHL7Hierarchy {

    @Test
    fun testLoadHierarchy() {
        val msg = this::class.java.getResource("/Lyme_HappyPath.txt").readText()
        val profile = this::class.java.getResource("/BasicProfile.json").readText()

        val tree = HL7HierarchyParser.parseMessageHierarchyFromJson(msg, profile)

        printTree(tree, "", 1)
    }
    
    private fun printTree(node: HL7Hierarchy, ident: String, line: Int) {
        println("($line) - $ident --> ${node.segment().substring(0,3)}")
        val list = node.children().toList()
        list.foreach { t ->
            printTree(t as HL7Hierarchy, "$ident   ", line+1)
        }
    }
}