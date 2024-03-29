package gov.cdc.dataExchange

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import open.HL7PET.tools.HL7StaticParser
import org.slf4j.LoggerFactory
import redis.clients.jedis.DefaultJedisClientConfig
import redis.clients.jedis.Jedis


class MMGValidator {
    companion object {
        val REDIS_CACHE_NAME = "temedehl7.redis.cache.windows.net"
        val REDIS_PWD = System.getenv("REDISCACHEKEY")
        val logger = LoggerFactory.getLogger(MMGValidator::class.simpleName)
    }
    val jedis = Jedis(REDIS_CACHE_NAME, 6380, DefaultJedisClientConfig.builder()
            .password(REDIS_PWD)
            .ssl(true)
            .build()
    )
    // Simple P
    fun validate(message: String, mmg: MMG): List<ValidationIssue> {
        val report = mutableListOf<ValidationIssue>()
        mmg.blocks.forEach { block ->
          block.elements.forEach { elem ->
              val msgValues = HL7StaticParser.getValue(message, elem.path)
              if (msgValues.isDefined && msgValues?.get() != null) {
                  checkVocab(elem, msgValues.get(), message, report)
              } //Found value in message
          }
        }
        return report
    }

    private fun checkVocab(elem: Element, msgValues: Array<Array<String>>, message: String, report:MutableList<ValidationIssue> ) {
        if (!elem.valueSetCode.isNullOrEmpty() && !"N/A".equals(elem.valueSetCode)) {
            logger.debug("Validating ${elem.valueSetCode}")
            //val concepts = retrieveValueSetConcepts(elem.valueSetCode)
            msgValues.forEachIndexed { outIdx, outArray ->
                outArray.forEachIndexed { inIdx, inElem ->
                    //if (concepts.filter { it.conceptCode == inElem }.isEmpty()) {
                    if (!checkConcept(elem.valueSetCode, inElem)) {
                        val lineNbr = getLineNumber(message, elem, outIdx)
                        val issue = ValidationIssue(getCategory(elem.mappings.hl7v251.usage), VALIDATION_ISSUE_TYPE.VOCAB, elem.name, elem.path, lineNbr,"Unable to find $inElem on ${elem.valueSetCode} on line $lineNbr" )
                        report.add(issue)
                        //println("Warning: Unable to find $inElem on ${elem.valueSetCode} on line $lineNbr")
                    } else {
                        logger.debug("$inElem is valid for ${elem.valueSetCode}")
                    }
                }//.forEach Inner Array
            } //.forEach Outer Array
        } //value Set code is empty
    }

    private fun getCategory(usage: String): String {
        return when (usage) {
            "R" -> "ERROR"
            else -> "WARNING"
        }
    }

    private fun getSegIdx(elem: Element): String {
        return when (elem.mappings.hl7v251.segmentType) {
            "OBX" -> "@3.1='${elem.mappings.hl7v251.identifier}'"
            else -> ""
        }
    }

    private fun getLineNumber(message: String, elem: Element, outArrayIndex: Int): Int {
        val allSegs = HL7StaticParser.getListOfMatchingSegments(message, elem.mappings.hl7v251.segmentType, getSegIdx(elem))
        var line = 0
        var forBreak = 0
        for ( k in allSegs.keys().toList()) {
            line = k as Int
            if (forBreak >= outArrayIndex) break
            forBreak++
        }
        return line
    }

    //Some Look ps are reused - storing them so no need to re-download them from Redis.
    private val valueSetMap = mutableMapOf<String, List<ValueSetConcept>>()
//    private val mapper = jacksonObjectMapper()
    @Throws(InvalidConceptKey::class)
    fun retrieveValueSetConcepts(key: String): List<ValueSetConcept> {
        if (valueSetMap[key] == null) {
            logger.debug("Retrieving $key from Redis")
            val conceptStr = jedis.get(key)
            if (conceptStr == null) {
                throw InvalidConceptKey("Unable to retrieve concept values for $key")
            }
            val listType = object : TypeToken<List<ValueSetConcept>>() {}.type
            valueSetMap[key] = Gson().fromJson(conceptStr, listType)
        }
        return valueSetMap[key]!!
    }

    fun checkConcept(key: String, concept: String):Boolean {
        return jedis.hexists(key, concept)
    }
}