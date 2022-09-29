
import gov.cdc.dex.hl7.MmgUtil
import open.HL7PET.tools.HL7StaticParser
import org.testng.annotations.Test

class MMGTest {

    @Test
    fun testLoadMMG() {
        val mmgName = "TBRD"
        val mmgJson = MmgUtil::class.java.getResource("/" + mmgName + ".json" ).readText()
        println(mmgJson)
    }

    @Test
    fun testMMGUtilGetMMG() {
        val testMsg = this::class.java.getResource("/Lyme_V1.0.2_TM_TC01.hl7").readText()
        val mmgs = MmgUtil.getMMGFromMessage(testMsg)
        mmgs.forEach { println(it)}
    }

    @Test
    fun testGetSegments() {
        val testMsg = this::class.java.getResource("/Lyme_V1.0.2_TM_TC01.hl7").readText()
        val mmgs = MmgUtil.getMMGFromMessage(testMsg)
        mmgs.forEach { mmg ->
            mmg.blocks.forEach { block ->
                block.elements.forEach { element ->
                    val segments = HL7StaticParser.getValue(testMsg, element.getSegmentPath())
                    println("--SEGMENT ${element.name}--")
                    if (segments.isDefined) {
                        segments.get().flatten().forEach { println(it) }
                        if (block.type in listOf("Repeat", "RepeatParentChild")) {
                            val allOBXs = segments.get().flatten().joinToString("\n")
                            val uniqueGroups = HL7StaticParser.getValue(allOBXs, "OBX-4")
                            if (uniqueGroups.isDefined) {
                                println("Unique Groups: " +uniqueGroups.get().flatten().distinct())
                            }
                        }
                    }
                    println("--END Seg ${element.name}")
                }
            }
        }

    }

    @Test
    fun testGetLineNumber() {
        val testMsg = this::class.java.getResource("/Lyme_V1.0.2_TM_TC01.hl7").readText()
        val dataTypeSegments = HL7StaticParser.getListOfMatchingSegments(testMsg, "OBX", "@3.1='INV930'")
        for ( k in dataTypeSegments.keys().toList()) {
           println(dataTypeSegments[k].get()[5])
        }

    }

    @Test
    fun testInvalidCode() {

    }
}