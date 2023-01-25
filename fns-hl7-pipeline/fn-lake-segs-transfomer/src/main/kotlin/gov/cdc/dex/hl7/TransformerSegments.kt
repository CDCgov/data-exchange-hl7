package gov.cdc.dex.hl7

import org.slf4j.LoggerFactory

// import com.google.gson.GsonBuilder

// import com.google.gson.JsonObject
// import com.google.gson.JsonParser


class TransformerSegments()  {

    companion object {
        val logger = LoggerFactory.getLogger(TransformerSegments::class.java.simpleName)
        // private val gson = Gson()
        // private val gsonWithNullsOn = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()

    } // .companion object


    fun hl7ToSegments( hl7Message: String) : List<Any?> {

        val hl7Arr = hl7Message.split("\n")

        logger.info(hl7Arr.toString())

        return listOf(hl7Message)
    } // .hl7ToSegments




} // .TransformerSql

