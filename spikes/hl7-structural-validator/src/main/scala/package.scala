package cdc.xlr

package object structurevalidator {

    val HL7_TEST_MESSAGES_LOCATION = "src/main/resources/HL7TestMessages/"

    // profiles available local
    val PROFILES_PHIN_SPEC_2_0 = "phin_spec_2_0"
    val PROFILES_PHIN_SPEC_3_0 = "phin_spec_3_0"
    val PROFILES_PHIN_SPEC_3_1 = "phin_spec_3_1"
    val PROFILES_PHIN_SPEC_3_2 = "phin_spec_3_2"

    // Default profile file names
    val PROFILES_CONSTRAINTS_DEFAULT_FILE_NAME = "Constraints.xml"
    val PROFILES_PROFILE_DEFAULT_FILE_NAME = "Profile.xml"
    val PROFILES_VALUESETS_DEFAULT_FILE_NAME = "ValueSets.xml"

} // .package object 