package gov.cdc.dataExchange


import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gov.cdc.dex.util.JsonHelper
import open.HL7PET.tools.HL7StaticParser
import org.junit.jupiter.api.Test
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis
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


    val REDIS_PWD = System.getenv("REDIS_CACHE_KEY")

    val jedis = Jedis(MMGValidator.REDIS_CACHE_NAME, 6380, DefaultJedisClientConfig.builder()
        .password(REDIS_PWD)
        .ssl(true)
        .build()
    )
    @Test
    fun testRetrieveMMG() {
        val keys = jedis.keys("mmg:*")
        println("keys:")
        println(keys)

        val mmg = jedis.get("mmg:tbrd")
        println("\n\n\nMMG:Lyme")
        println(mmg.substring(0,100))
        val mmgObj = Gson().fromJson(mmg, MMG::class.java)
        println(mmgObj)
    }

    @Test
    fun testRetrieveConditions() {
        val keys = jedis.keys("condition:*")
        println("keys:")
        println(keys)

        val mmgMapping = jedis.get("condition:10250")
        println("\n\n\ncondition: 10250")
        println(mmgMapping)

        val msh21_3 = "lyme_tbrd_mmg_v1.0"

        val mmgMapJson: JsonObject = JsonParser.parseString(mmgMapping) as JsonObject
        val mmgArray  = JsonHelper.getValueFromJson("mmg_maps", mmgMapJson).asJsonObject
        val mmgLyme = mmgArray[msh21_3]
//        val mmgObj = Gson().fromJson(mmg, MMG::class.java)
        println("mmg: $mmgLyme")
    }
}