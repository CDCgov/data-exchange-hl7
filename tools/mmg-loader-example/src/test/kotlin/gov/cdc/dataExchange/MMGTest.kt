package gov.cdc.dataExchange


import com.google.gson.Gson
import open.HL7PET.tools.HL7StaticParser
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class MMGTest {

    @Test
    fun loadMMG() {
        val mmg = this::class.java.getResource("/tbrd.json").readText()

         val mmgFromJson = Gson().fromJson(mmg, MMG::class.java)
//        println(mmgFromJson)

        var count = 0
        mmgFromJson.blocks.forEach {block ->
            println("Block: ${block.name} -  ${block.elements.count()}")
            block.elements.forEach { elem ->
                println("\t${elem.name}: ${elem.path}" )
                count++
            }
        }
        println("Found $count elements")

    }
    @Test
    fun testValidateVocab() {
        val time = measureTimeMillis {
            val msg = this::class.java.getResource("/testMessage.hl7").readText()
            val mmg = this::class.java.getResource("/mmgs/GENERIC_MMG_V2.0.json").readText()

            val mmgFromJson = Gson().fromJson(mmg, MMG::class.java)

            val validator = MMGValidator()
            val report = validator.validate(msg, mmgFromJson)

//            println(report)
        }
        println("Validation took $time")

    }


    @Test
    fun extractPath() {
        val identifier = "N/A: MSH-21"
        val regex = "[A-Z]{3}\\-[0-9]*".toRegex()
        val path = regex.find(identifier)
        println(path?.value)

    }

    @Test
    fun testFindSegLine() {
        val msg = this::class.java.getResource("/testMessage.hl7").readText()
        val allSegs = HL7StaticParser.getListOfMatchingSegments(msg, "OBX", "@3.1='77993-4'")
        val keys = allSegs.keySet().toList()


        for (k in keys) {
            println(k)
        }

    }
    @Test
    fun testFindValue() {
        val msg = this::class.java.getResource("/testMessage.hl7").readText()
        val allValues = HL7StaticParser.getFirstValue(msg, "OBX[@3.1='77993-4']-5[2].1")
        println(allValues)
    }
}