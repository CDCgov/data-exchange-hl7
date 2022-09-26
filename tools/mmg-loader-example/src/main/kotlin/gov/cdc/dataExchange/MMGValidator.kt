package gov.cdc.dataExchange

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
        mmg.result.blocks.forEach { block ->
          block.elements.forEach { elem ->
              val msgValues = HL7StaticParser.getValue(message, elem.path)
              if (msgValues.isDefined && msgValues?.get() != null) {
                  if (!elem.valueSetCode.isNullOrEmpty() && !"N/A".equals(elem.valueSetCode)) {
                      logger.debug("Validating ${elem.valueSetCode}")
                      val concepts = retrieveValueSetConcepts(elem.valueSetCode)
                      msgValues.get().forEachIndexed { outIdx, outArray ->
                          outArray.forEachIndexed { inIdx, inElem ->
                              if (concepts.filter { it.conceptCode == inElem }.isEmpty()) {
                                 val lineNbr = getLineNumber(message, elem, outIdx)
                                  val issue = ValidationIssue(getCategory(elem.mappings.hl7v251.usage), VALIDATION_ISSUE_TYPE.VOCAB, elem.name, elem.path, lineNbr,"Unable to find $inElem on ${elem.valueSetCode} on line $lineNbr" )
                                  report.add(issue)
                                  //println("Warning: Unable to find $inElem on ${elem.valueSetCode} on line $lineNbr")
                              } else {
                                  logger.debug(" $inElem is valid for ${elem.valueSetCode}")
                              }
                          }//.forEach Inner Array
                      } //.forEach Outer Array
                  } //value Set code is empty
              } //Found value in message
          }
        }
        return report
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

    fun retrieveValueSetConcepts(key: String): List<ValueSetConcept> {
        val conceptStr = jedis.get(key)
        val mapper = jacksonObjectMapper()
        return mapper.readValue(conceptStr, mapper.typeFactory.constructCollectionType(
            MutableList::class.java,
            ValueSetConcept::class.java
        ))
    }
}