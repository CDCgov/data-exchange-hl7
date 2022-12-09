package gov.cdc.dex.hl7

// import org.slf4j.LoggerFactory
// import gov.cdc.dex.redisModels.MMG
// import gov.cdc.dex.redisModels.Block
// import gov.cdc.dex.redisModels.Element
// import gov.cdc.dex.redisModels.ValueSetConcept

// import gov.cdc.dex.hl7.model.PhinDataType

// import gov.cdc.dex.util.StringUtils

// import com.google.gson.Gson 
// import com.google.gson.GsonBuilder
// import com.google.gson.reflect.TypeToken

import  gov.cdc.dex.azure.RedisProxy 

class TransformerSql(val redisProxy: RedisProxy)  {

    val redisClient = redisProxy.getJedisClient()

    companion object {
        // val logger = LoggerFactory.getLogger(TransformerSql::class.java.simpleName)
        // private val gson = Gson()
        // private val gsonWithNullsOn: Gson = GsonBuilder().serializeNulls().create() //.setPrettyPrinting().create()

    } // .companion object

    fun toSqlModel() : Int {
        return 42
    }


} // .TransformerSql

