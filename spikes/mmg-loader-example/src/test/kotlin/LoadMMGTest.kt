
import com.google.gson.Gson
import gov.cdc.dataExchange.MMG
import org.junit.jupiter.api.Test



class LoadMMGTest {

    val MMG_LIST = arrayOf("ARBO_1.2", "ARBO_CASE_MAP_V1.0", "GEN_CASE_MAP_V1.0", "SUMM_CASE_MAP_V1.0"
    ,"GENERIC_MMG_V2.0", "TB_CASE_MAP_V2.0", "VAR_CASE_MAP_V1.0", "VAR_CASE_MAP_V2.0")
    @Test
    fun loadMMG() {
        for (mmgName in MMG_LIST) {
            val mmg = this::class.java.getResource("/mmgs/${mmgName}.json").readText()

//        val mapper = jacksonObjectMapper()
//        val mmgFromJson = mapper.readValue(mmg, MMG::class.java)
            val mmgFromJson = Gson().fromJson(mmg, MMG::class.java)
//        println(mmgFromJson)
            println("=======================================\n\nMMG: $mmgName \n")
            var count = 0
            mmgFromJson.blocks.forEach { block ->
                println("Block: ${block.name} -  ${block.elements.count()}")
                block.elements.forEach { elem ->
                 //   println("\t${elem.name}: ${elem.mappings.hl7v251.path}")
                    count++
                }
            }
            println("Found $count elements")
        }
    }

    @Test
    fun extractPath() {
        val identifier = "N/A: MSH-21"
        val regex = "[A-Z]{3}\\-[0-9]*".toRegex()
        val path = regex.find(identifier)
        println(path?.value)

    }
}