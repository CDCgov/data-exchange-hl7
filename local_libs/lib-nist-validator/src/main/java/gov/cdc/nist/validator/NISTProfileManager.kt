package gov.cdc.nist.validator

import gov.nist.validation.report.Entry
import gov.nist.validation.report.Report
import hl7.v2.profile.XMLDeserializer
import hl7.v2.validation.SyncHL7Validator
import hl7.v2.validation.content.DefaultConformanceContext
import hl7.v2.validation.vs.ValueSetLibraryImpl

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.logging.Logger

/**
 *
 *
 * @Created - 4/26/20
 * @Author Marcelo Caldas mcq1@cdc.gov
 */

class NISTProfileManager(profileFetcher: ProfileFetcher, val profile: String) {
    companion object {
        private val logger =  Logger.getLogger(NISTProfileManager::class.java.name)


        private const val VALID_MESSAGE_STATUS = "VALID_MESSAGE"
        private const val STRUCTURE_ERRORS_STATUS = "STRUCTURE_ERRORS"
        private const val CONTENT_ERRORS_STATUS = "CONTENT_ERRORS"
        private const val ERROR_CLASSIFICATION = "Error"
        private const val WARNING_CLASSIFICATION = "Warning"

        private const val VALUE_SET_ENTRIES = "value-set"
        private const val STRUCTURE_ENTRIES = "structure"
        private const val CONTENT_ENTRIES = "content"

    }

    private val validator: SyncHL7Validator


    init {
        logger.info("AUDIT:: Loading profile $profile")
        try {
            val profileXML = profileFetcher.getFileAsInputStream("$profile/PROFILE.xml")
            // The get() at the end will throw an exception if something goes wrong
             val profileX = XMLDeserializer.deserialize(profileXML).get()
            // get ConformanceContext
            val contextXML1 = profileFetcher.getFileAsInputStream("$profile/CONSTRAINTS.xml")
            // The second conformance context XML file
    //        val confContexts = mutableListOf<InputStream>()
            val confContexts = mutableListOf(contextXML1)
    //        try {
    //            val contextXML2 = profileFetcher.getFileAsInputStream("$profile/PREDICATES.xml")
    //            confContexts.add(contextXML2)
    //            //Add predicates to confContexts...
    //        } catch (e: Exception) {
    //            logger.fine("No Predicate Available for group $profile. Ignoring Predicate.")
    //            //No predicate available... ignore file...
    //        }
            // The get() at the end will throw an exception if something goes wrong
            val conformanceContextTry = DefaultConformanceContext.apply(confContexts.toList())
            val conformanceContext = conformanceContextTry.get()
            // get ValueSetLibrary
            val vsLibXML = profileFetcher.getFileAsInputStream("$profile/VALUESETS.xml")
            val valueSetLibrary = ValueSetLibraryImpl.apply(vsLibXML).get()
            validator = SyncHL7Validator(profileX, valueSetLibrary, conformanceContext)
        } catch (e: Error) {
            logger.warning("UNABLE TO READ PROFILE: " + profile + " with error:\n" + e.message)
            e.printStackTrace();
            throw  InvalidFileException("Unable to parse profile file with error: " + e.message)
        }
    }

    @Throws(java.lang.Exception::class)
    fun validate(hl7Message: String): NistReport {
        val messageIds = validator.profile().messages().keySet().iterator()
        val msId = messageIds.next()
        val report = validator.check(hl7Message, msId)

        return filterAndConvert(report)
    }

    private fun filterAndConvert(report: Report): NistReport {
        val nist = NistReport()
        val errCount: MutableMap<String, AtomicInteger> = mutableMapOf()
        val warCount: MutableMap<String, AtomicInteger> = mutableMapOf()
        val valMap = report.entries
        val filteredMap: MutableMap<String, List<Entry>> = mutableMapOf()

        valMap.forEach { (k: String, v: List<Entry>) ->
            errCount[k] = AtomicInteger()
            warCount[k] = AtomicInteger()
            val filteredContent: MutableList<Entry> = mutableListOf()

            v.forEach(Consumer { entry: Entry ->
                if (entry.classification == ERROR_CLASSIFICATION || entry.classification == WARNING_CLASSIFICATION) {
                    filteredContent.add(entry)
                    if (entry.classification == WARNING_CLASSIFICATION)
                        warCount[k]?.getAndIncrement()
                    if (entry.classification == ERROR_CLASSIFICATION)
                        errCount[k]?.getAndIncrement()
                }
            })
            filteredMap[k] = filteredContent
        }
        var status = VALID_MESSAGE_STATUS
        if (errCount[STRUCTURE_ENTRIES]!!.get() > 0) {
            status = STRUCTURE_ERRORS_STATUS
        } else if (errCount[CONTENT_ENTRIES]!!.get() > 0 || errCount[VALUE_SET_ENTRIES]!!.get() > 0) {
            status = CONTENT_ERRORS_STATUS
        }

        nist.entries.structure = filteredMap[STRUCTURE_ENTRIES] ?: listOf()
        nist.entries.content = filteredMap[CONTENT_ENTRIES] ?: listOf ()
        nist.entries.valueset = filteredMap[VALUE_SET_ENTRIES] ?: listOf()
        nist.transferErrorCounts(errCount)
        nist.transferWarningCounts(warCount)
        nist.status = status
        return nist
    }
}