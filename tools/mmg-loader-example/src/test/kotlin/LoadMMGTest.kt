
import com.google.gson.Gson
import gov.cdc.dataExchange.MMG
import org.junit.jupiter.api.Test

//import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class LoadMMGTest {

    @Test
    fun loadMMG() {
        val mmg = this::class.java.getResource("/lyme.json").readText()

//        val mapper = jacksonObjectMapper()
        val mmgFromJson = Gson().fromJson(mmg, MMG::class.java)

//        println(mmgFromJson)

        var count = 0
        mmgFromJson.result.blocks.forEach {block ->
            println("Block: ${block.name} -  ${block.elements.count()}")
            block.elements.forEach { elem ->
                println("\t${elem.name}: ${elem.mappings.hl7v251.path}" )
                count++
            }
        }
        println("Found $count elements")

    }

    @Test
    fun extractPath() {
        val identifier = "N/A: MSH-21"
        val regex = "[A-Z]{3}\\-[0-9]*".toRegex()
        val path = regex.find(identifier)
        println(path?.value)

    }
}