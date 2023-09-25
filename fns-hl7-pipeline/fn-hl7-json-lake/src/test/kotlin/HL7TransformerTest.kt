import gov.cdc.dex.hl7.FunctionConfig
import gov.cdc.hl7.bumblebee.HL7JsonTransformer
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class HL7TransformerTest {

    //OBX Test 13-24 segments
    @Test
    fun testBuildJson() {
        val text = this::class.java.getResource("hl7Messages/hl7Message.txt").readText()
        val bumblebee = HL7JsonTransformer.getTransformerWithResource(text, FunctionConfig.PROFILE_FILE_PATH)
        val jsonObject = bumblebee.transformMessage()
        assertTrue(jsonObject.toString().contains("effective_date_of_reference_range"))
        assertTrue(jsonObject.toString().contains("EffectiveDate1"))
        assertTrue(jsonObject.toString().contains("user_defined_access_checks"))
        assertTrue(jsonObject.toString().contains("UserDefinedAccessChecks1"))
        assertTrue(jsonObject.toString().contains("date_time_of_the_observation"))
        assertTrue(jsonObject.toString().contains("Date/TimeoftheObservation1"))
        assertTrue(jsonObject.toString().contains("producers_id"))
        assertTrue(jsonObject.toString().contains("ProducersID1"))
        assertTrue(jsonObject.toString().contains("responsible_observer"))
        assertTrue(jsonObject.toString().contains("ResponsibleObserver1"))
        assertTrue(jsonObject.toString().contains("observation_method"))
        assertTrue(jsonObject.toString().contains("ObservationMethod1"))
        assertTrue(jsonObject.toString().contains("equipment_instance_identifier"))
        assertTrue(jsonObject.toString().contains("EquipmentInstanceIdentifier1"))
        assertTrue(jsonObject.toString().contains("date_time_of_the_analysis"))
        assertTrue(jsonObject.toString().contains("Date/TimeoftheAnalysis1"))
        assertTrue(jsonObject.toString().contains("observation_site"))
        assertTrue(jsonObject.toString().contains("Observation Site1"))
        assertTrue(jsonObject.toString().contains("observation_instance_identifier"))
        assertTrue(jsonObject.toString().contains("Observation Instance Identifier1"))
        assertTrue(jsonObject.toString().contains("mood_code"))
        assertTrue(jsonObject.toString().contains("Mood Code1"))
        assertTrue(jsonObject.toString().contains("mood_code"))
        assertTrue(jsonObject.toString().contains("performing_organization_name"))
        assertTrue(jsonObject.toString().contains("Performing Organization Address1"))
    }
}