package gov.cdc.dex.hl7

import com.google.gson.Gson

import gov.cdc.dex.redisModels.MMG


import gov.cdc.hl7.HL7StaticParser
import org.slf4j.LoggerFactory
import java.util.*

import  gov.cdc.dex.mmg.MmgUtil as MmgUtilLib
import  gov.cdc.dex.azure.RedisProxy 

class MmgUtil(val redisProxy: RedisProxy)  {

    companion object {
        val logger = LoggerFactory.getLogger(MmgUtil::class.java.simpleName)

        const val PATH_MSH_21_2_1 = "MSH-21[2].1" // Gen 
        const val PATH_MSH_21_3_1 = "MSH-21[3].1" // Condition
        const val EVENT_CODE_PATH = "OBR[@4.1='68991-9']-31.1"

        private val gson = Gson()
    } // .companion

        
        @Throws(Exception::class)
        fun getMMGFromMessage(message: String, reportingJurisdiction: String): Array<MMG> {

            val msh21_2 = extractValue(message, PATH_MSH_21_2_1).lowercase(Locale.getDefault())
            val msh21_3 = extractValue(message, PATH_MSH_21_3_1).lowercase(Locale.getDefault())
            val eventCode = extractValue(message, EVENT_CODE_PATH)

            val mmgUtilLib = MmgUtilLib(redisProxy)

            return mmgUtilLib.getMMGs(msh21_2, msh21_3, eventCode, reportingJurisdiction) 
        } // .getMMGFromMessage

        private fun extractValue(msg: String, path: String):String  {
            val value = HL7StaticParser.getFirstValue(msg, path)
            return if (value.isDefined) value.get() //throw Exception("Error extracting $path from HL7 message")
                else ""
        } // .extractValue


} // .MmgUtil

