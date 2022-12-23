package gov.cdc.nist.validator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @Created - 4/26/20
 * @Author Marcelo Caldas mcq1@cdc.gov
 */
public class ProfileManagerTest {

    @Test
    public void testValidateStructureErrors() {
        try {
            //ProfileManager nistValidator = new ProfileManager(new ResourceFileFetcher(), "/TEST_PROF");
            NISTProfileManager nistValidator = new NISTProfileManager(new ResourceFileFetcher(), "/NND_ORU_V2.0");

            var nist = nistValidator.validate(getTestFile("hl7TestMessage.txt"));
            System.out.println("nist.getStatus() = " + nist.getStatus());
            System.out.println("nist.getErrorCounts() = " + nist.getErrorCounts());
            System.out.println("nist.getErrorCounts().getStructure() = " + nist.getErrorCounts().getStructure());
            System.out.println("nist.getErrorCounts().getValueset() = " + nist.getErrorCounts().getValueset());
            System.out.println("nist.getErrorCounts().getContent() = " + nist.getErrorCounts().getContent());
            System.out.println("nist.getWarningCounts() = " + nist.getWarningcounts());
            System.out.println("nist = " + nist);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testValidateStrcutureValid() {
            try {
                NISTProfileManager nistValidator = new NISTProfileManager(new ResourceFileFetcher(), "/NOTF_ORU_V3.0");
//                ProfileManager nistValidator = new ProfileManager(new ResourceFileFetcher(), "/NOTF_ORU_V3.0");

                var nist = nistValidator.validate(getTestFile("hl7StructureValid.txt"));
                System.out.println("nist.getStatus() = " + nist.getStatus());
                System.out.println("nist.getErrorCounts() = " + nist.getErrorCounts());
                System.out.println("nist.getErrorCounts().getStructure() = " + nist.getErrorCounts().getStructure());
                System.out.println("nist.getErrorCounts().getValueset() = " + nist.getErrorCounts().getValueset());
                System.out.println("nist.getErrorCounts().getContent() = " + nist.getErrorCounts().getContent());
                System.out.println("nist.getWarningcounts() = " + nist.getWarningcounts());
                nist.getEntries().getStructure().forEach(it -> System.out.println(it));

            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    @Test
    public void testInvalidProfile() {
        try {
            new NISTProfileManager(new ResourceFileFetcher(), "/INVALID_PROFILE");
        } catch (Exception e) {
            System.out.println("Exception properly handled.");
        }
    }

    @Test
    public void testIncompleteProfile() {
        try {
            new NISTProfileManager(new ResourceFileFetcher(), "/INCOMPLETE_PROFILE");
        } catch (Exception e) {
            System.out.println("e.getMessage() = " + e.getMessage());
            assert(true);
        }
    }

    private String getTestFile(String filename) throws IOException {
        String fileName = filename;
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return new String(Files.readAllBytes(file.toPath()));
    }
}