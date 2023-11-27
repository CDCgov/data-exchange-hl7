package gov.cdc.dex.hl7.pipeline

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals

import org.junit.jupiter.api.Test
import java.io.File

class MainTest{

    @Test
    fun testPayloadsExist(){

    }
    @Test
    fun testMessageInfo(){

    }
    @Test
    fun testSummaryWithProblemAttribute(){
    }
    @Test
    fun testRequiredFieldsMSHCase(){
        val pathToNewPayloadForCaseFile = "src/test/resources/new-payloads/case.json"
        val newPayload = File(pathToNewPayloadForCaseFile)

        val pathToVerifiedPayloadForCaseFile = "src/test/resources/verified-payloads/case.json"
        val verifiedPayload = File(pathToVerifiedPayloadForCaseFile)

        if (newPayload.exists() && verifiedPayload.exists()) {
            val jsonMapper = jacksonObjectMapper()
            val jsonNewPayload:JsonNode = jsonMapper.readTree(newPayload)
            val jsonVerifiedPayload:JsonNode = jsonMapper.readTree(verifiedPayload)


            val fieldsToCompare = setOf("file_separator","encoding_characters",
                "sending_application","message_type","message_control_id",
                "processing_id","version_id")

            for (field in fieldsToCompare) {
                val fieldFromNewPayload = jsonNewPayload[field]
                val fieldFromVerifiedPayload = jsonVerifiedPayload[field]

                assertEquals(fieldFromNewPayload, fieldFromVerifiedPayload , "Required fields for MSH $field should match!")

            }


        }

    }
    @Test
    fun testInvalidMessageWithMissingMSH3(){
    }
    @Test
    fun testInvalidMessageWithMissingMSH4(){
    }
    @Test
    fun testInvalidMessageWithMissingMSH9(){
    }
    @Test
    fun testInvalidMessageWithMissingMSH12(){
    }
    @Test
    fun testCardinalityForMSH2(){
    }
    @Test
    fun testCardinalityForMSH2WithValueLengthMoreThan4Characters(){
    }
    @Test
    fun testBatchMessageCountAgainstActualMessagesInBatch(){
        //should fail if counts do not match
    }
    @Test
    fun testForEmptyBatches(){
    }
    @Test
    fun testForInvalidBatchDueToMissingFHSSegment(){
    }
    @Test
    fun testForInvalidBatchDueToMissingBHSSegment(){
    }
    @Test
    fun testForInvalidBatchDueToMissingBTSSegment(){
    }
    @Test
    fun testForInvalidBatchCountInBTSSegment(){
        //should fail
    }

    @Test
    fun testRequiredFieldsPIDCase(){

    }

    @Test
    fun testOBX2HasRightDatatypeInOBX5Case(){

    }
    @Test
    fun testOBX2HasIncorrectDatatypeInOBX5Case(){

    }

    @Test
    fun testOBX1ValueShouldBeUnique(){

    }
    @Test
    fun testOBX1ValueShouldBeSequential(){

    }
    @Test
    fun testPIDSocialSecurityCase(){
        //should be redacted
    }
    @Test
    fun testPIDDateOfBirthCase(){
        //should be redacted
    }
    @Test
    fun testPIDNameAndAddressCase(){
        //should be redacted
    }
    @Test
    fun testUniqueOBXWithSameOBX3AndNullOBX4Case(){
        //failure message
    }
    @Test
    fun testUniqueOBXWithSameOBX3AndDifferentOBX4Case(){
        //failure
    }
    @Test
    fun testWithMissingMessageProfileIdentifierValueCase(){
        //failure
    }

    @Test
    fun testRequiredFieldsPIDElr(){

    }
    @Test
    fun testOBX2HasRightDatatypeInOBX5Elr(){

    }
    @Test
    fun testOBX2HasIncorrectDatatypeInOBX5Elr(){

    }

    @Test
    fun testPIDSocialSecurityElr(){
        //should be redacted
    }
    @Test
    fun testPIDDateOfBirthElr(){
        //should be redacted
    }
    @Test
    fun testPIDNameAndAddressElr(){
        //should be redacted
    }
    @Test
    fun testUniqueOBXWithSameOBX3AndNullOBX4Elr(){
        //failure message
    }
    @Test
    fun testUniqueOBXWithSameOBX3AndDifferentOBX4Elr(){
        //failure
    }



}