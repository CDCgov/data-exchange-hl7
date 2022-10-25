package gov.cdc.nist.validator;


import gov.nist.validation.report.Entry;
import gov.nist.validation.report.Report;
import hl7.v2.profile.Profile;
import hl7.v2.profile.XMLDeserializer;
import hl7.v2.validation.SyncHL7Validator;
import hl7.v2.validation.content.ConformanceContext;
import hl7.v2.validation.content.DefaultConformanceContext;
import hl7.v2.validation.vs.ValueSetLibrary;
import hl7.v2.validation.vs.ValueSetLibraryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.collection.Iterator;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class ProfileManager {

    private static final String VALID_MESSAGE_STATUS = "VALID_MESSAGE";
    private static final String STRUCTURE_ERRORS_STATUS = "STRUCTURE_ERRORS";
    private static final String CONTENT_ERRORS_STATUS = "CONTENT_ERRORS";
    private static final String ERROR_CLASSIFICATION = "Error";
    private static final String WARNING_CLASSIFICATION = "Warning";

    private static final String VALUE_SET_ENTRIES = "value-set";
    private static final String STRUCTURE_ENTRIES = "structure";
    private static final String CONTENT_ENTRIES = "content";
    private static final String error_count = "error-count";
    private static final String warning_count = "warning-count";

    private final SyncHL7Validator validator;


    public  ProfileManager(ProfileFetcher profileFetcher, String profile) throws InvalidFileException {
        Logger logger = LoggerFactory.getLogger(ProfileManager.class.getName());
        try {
            logger.info("AUDIT:: Loading profile " + profile);
            InputStream profileXML = profileFetcher.getFileAsInputStream(profile + "/PROFILE.xml");
            // The get() at the end will throw an exception if something goes wrong
            Profile profileX = XMLDeserializer.deserialize(profileXML).get();
            // get ConformanceContext
            InputStream contextXML1 = profileFetcher.getFileAsInputStream(profile + "/CONSTRAINTS.xml");
            // The second conformance context XML file
            List<InputStream> confContexts = new ArrayList<>(Collections.singletonList(contextXML1));
            try {
                InputStream contextXML2 = profileFetcher.getFileAsInputStream(profile + "/PREDICATES.xml");
                confContexts.add(contextXML2);
                //Add predicates to confContexts...
            } catch (Exception e) {
                logger.debug("No Predicate Available for group " + profile + ". Ignoring Predicate.");
                //No predicate available... ignore file...
            }

            // The get() at the end will throw an exception if something goes wrong
            ConformanceContext conformanceContext = DefaultConformanceContext.apply(confContexts).get();
            // get ValueSetLibrary
            InputStream vsLibXML = profileFetcher.getFileAsInputStream(profile + "/VALUESETS.xml");
            ValueSetLibrary valueSetLibrary = ValueSetLibraryImpl.apply(vsLibXML).get();

            validator = new SyncHL7Validator(profileX, valueSetLibrary, conformanceContext);
        } catch (Error e) {
            logger.warn("UNABLE TO READ PROFILE: " + profile + " with error:\n" + e.getMessage());
            throw new InvalidFileException("Unable to parse profile file..." + e.getMessage());
        }
    }


    public NistReport validate(String hl7Message) throws Exception {
        Iterator<String> messageIds = validator.profile().messages().keySet().iterator();
        String msId = messageIds.next();
        Report report = validator.check(hl7Message, msId);
        Map filteredReport =   filter(report);

        NistReport nist = new NistReport();
        HashMap errors = (HashMap) filteredReport.get("entries");
        if (errors.containsKey("structure"))
            nist.getEntries().setStructure((ArrayList<Entry>) errors.get("structure"));
        if (errors.containsKey("value-set"))
            nist.getEntries().setValueset((ArrayList<Entry>) errors.get("value-set"));
        if (errors.containsKey("content"))
            nist.getEntries().setContent((ArrayList<Entry>) errors.get("content"));

        nist.transferErrorCounts ((HashMap) filteredReport.get("error-count"));
        nist.transferWarningCounts((HashMap) filteredReport.get("warning-count"));

        nist.setStatus((String) filteredReport.get("status"));
        return nist;
    }


    protected  Map<String, Object> filter(Report report)  {
        Map<String, AtomicInteger>  erCount = new HashMap<>();
        Map<String, AtomicInteger>  warCount = new HashMap<>();
        Map<String, List<Entry>> valMap = report.getEntries();
        Map<String, Object> filteredMap = new HashMap<>();
        Map<String, Object> validationResultsMap = new HashMap<>();
        valMap.forEach((k, v) -> {
            erCount.put(k, new AtomicInteger());
            warCount.put(k, new AtomicInteger());
            List<Entry> filteredContent = new ArrayList<>();
            v.forEach(val -> {
                if (val.getClassification().equals(ERROR_CLASSIFICATION) || val.getClassification().equals(WARNING_CLASSIFICATION)){
                    filteredContent.add(val);
                    if(val.getClassification().equals(WARNING_CLASSIFICATION))
                        warCount.get(k).getAndIncrement();
                    if(val.getClassification().equals(ERROR_CLASSIFICATION))
                        erCount.get(k).getAndIncrement();
                }
            });
            filteredMap.put(k, filteredContent);
        });
        String status = VALID_MESSAGE_STATUS;
        if (erCount.get(STRUCTURE_ENTRIES).get() > 0) {
            status = STRUCTURE_ERRORS_STATUS;
        } else if (erCount.get(CONTENT_ENTRIES).get() > 0 || erCount.get(VALUE_SET_ENTRIES).get() > 0) {
            status = CONTENT_ERRORS_STATUS;
        }
        validationResultsMap.put(error_count, erCount);
        validationResultsMap.put(warning_count, warCount);
        validationResultsMap.put("entries", filteredMap);
        validationResultsMap.put("status", status);
        return validationResultsMap;
    }
}
