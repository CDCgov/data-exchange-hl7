package gov.cdc.nist.validator;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class VALID_PROFILE_CONFIGSTest {

    @Test
    public void testStrings() {
        assertEquals(VALID_PROFILE_CONFIGS.valueOf("PROFILE"), VALID_PROFILE_CONFIGS.PROFILE);
        assertEquals(VALID_PROFILE_CONFIGS.valueOf("CONSTRAINTS"), VALID_PROFILE_CONFIGS.CONSTRAINTS);
        assertEquals(VALID_PROFILE_CONFIGS.valueOf("VALUESETS"), VALID_PROFILE_CONFIGS.VALUESETS);
        assertEquals(VALID_PROFILE_CONFIGS.valueOf("PREDICATES"), VALID_PROFILE_CONFIGS.PREDICATES);
    }

    String[] fileNames = {"Profile.xml", "constraints.xml", "valuesets.xml"};
    VALID_PROFILE_CONFIGS[] VALID_VALUES = {VALID_PROFILE_CONFIGS.PROFILE, VALID_PROFILE_CONFIGS.CONSTRAINTS, VALID_PROFILE_CONFIGS.VALUESETS};
    @Test
    public void testFileNames() {
        int i = 0;
        for (String f: fileNames) {
            assertEquals(VALID_PROFILE_CONFIGS.valueOf(f.toUpperCase().substring(0, f.lastIndexOf("."))), VALID_VALUES[i++]);
        }
    }


    @Test
    public void testInvalidNames() {
        try {
            System.out.println(VALID_PROFILE_CONFIGS.valueOf("Invalid"));
            assertFalse(false);
        } catch (IllegalArgumentException e){
            assert(true);
            System.out.println("Exception properly thrown!");


        }
    }

}