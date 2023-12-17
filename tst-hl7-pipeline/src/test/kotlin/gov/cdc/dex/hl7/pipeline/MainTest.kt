package gov.cdc.dex.hl7.pipeline

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals

import org.junit.jupiter.api.Test
import java.io.File

class MainTest{

    @Test
    fun testPayloadsExist(){
        // choose any payload

    }
    @Test
    fun testMessageInfo(){
        //choose  any payload

    }
    @Test
    fun testSummaryWithProblemAttribute(){
        //read any error payload
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
        //use COVID19_Missing_MSH3.txt
    }
    @Test
    fun testInvalidMessageWithMissingMSH4(){
        //use  COVID19_Missing_MSH4.txt
    }
    @Test
    fun testInvalidMessageWithMissingMSH9(){
        //use  COVID19_Missing_MSH9.txt
    }
    @Test
    fun testInvalidMessageWithMissingMSH12(){
        //use  COVID19_Missing_MSH12.txt
    }
    @Test
    fun testCardinalityForMSH2(){
        // use any valid message
    }
    @Test
    fun testCardinalityForMSH2WithValueLengthMoreThan4Characters(){
        // invalid case create from any message
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
    fun testPhlipFluWithDataTypeNM(){
        //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
    }
    @Test
    fun testPhlipFluWithDataTypeTS(){
        //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
    }
    @Test
    fun testPhlipFluWithDataTypeTM(){
        //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
    }
    @Test
    fun testPhlipFluWithDataTypeDT(){
        //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
    }
    @Test
    fun testPhlipFluWithDataTypeCWE(){
        //use PHLIP_FLU_DataType_CWE.txt
    }
    @Test
    fun testPhlipFluWithDataTypeFT(){
        //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
    }
    @Test
    fun testPhlipFluWithDataTypeTX(){
        //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
    }
    @Test
    fun testPhlipFluWithDataTypeSN(){
        //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
    }
    @Test
    fun testPhlipFluWithDataTypeCX(){
        //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
    }
    @Test
    fun testPhlipFluWithDataTypeCE(){
        //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
    }
    @Test
    fun testPhlipFluWithDataTypeED(){
        //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt

    }
    @Test
    fun testPhlipFluWithDataTypeRP(){
        // need to find out
    }
    @Test
    fun testVPDWithDataTypeNM(){
        // use PHLIP_VPD_VALID.txt OBX3

    }
    @Test
    fun testVPDWithDataTypeSN(){
        //useuse PHLIP_VPD_VALID OBX56, 57,58

    }
    @Test
    fun testVPDWithDataTypeTS(){
        //use PHLIP_VPD_VALID OBX35
    }
    @Test
    fun testVPDWithDataTypeTM(){
        //use PHLIP_VPD_VALID  add TM
    }
    @Test
    fun testVPDWithDataTypeDT(){
        //use PHLIP_VPD_VALID OBX 37, 38, 39
    }
    @Test
    fun testVPDWithDataTypeCWE(){
        //use PHLIP_VPD_VALID OBX 40, 41, 42
    }
    @Test
    fun testVPDWithDataTypeFT(){
        //PHLIP_VPD_VALID_DataType_FT.txt OBX175
    }
    @Test
    fun testVPDWithDataTypeTX(){
        //use PHLIP_VPD_VALID OBX 102
    }
    @Test
    fun testVPDWithDataTypeED(){
        //PHLIP_VPD_DataType_ED.txt
    }
    @Test
    fun testVPDWithDataTypeRP(){
        // need to find out
    }

    @Test
    fun testRequiredFieldsPIDCase(){
        //PHLIP_Salm_PID_Required_Fields_Case.txt
    }

    @Test
    fun testOBX2HasRightDatatypeInOBX5Case(){
        // use PHLIP_DataType_DT_CASE.txt
    }
    @Test
    fun testOBX2HasIncorrectDatatypeInOBX5Case(){
        // PHLIP_OBX2_CWE_OBX5_ST.txt OBR1 OBX1
    }

    @Test
    fun testOBX1ValueShouldBeUniqueCELR(){
        // use COVID-19_OBX1_Uniqueness_Test.txt

    }
    @Test
    fun testOBX1ValueShouldBeSequentialCELR(){
        //use COVID-19_OBX_Sequentional_Test.txt

    }
    @Test
    fun testSendingAndReceivingApplicationsCASE(){
        //use PHLIP_FLU_Receiving_Sending_Applications.txt
    }
    @Test
    fun testPIDSocialSecurityCase(){
        //PHLIP_FLU_PID_19.txt
    }
    @Test
    fun testPIDDateOfBirthCase(){
        // use PHLIP_FLU_PID_7_DateTimeOfBirth.txt
    }
    @Test
    fun testPIDNameAndAddressCase(){
        //use FDD_LIST_PID_Name_Address_CASE.txt
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
        //use COVID-19_PID_Required_Fields.txt
    }
    @Test
    fun testOBX2HasRightDatatypeInOBX5Elr(){
        //use COVID-19_OBX2CWE_OBX5_CWE.txt #OBX|6
    }
    @Test
    fun testOBX2HasIncorrectDatatypeInOBX5Elr(){
        //COVID-19_OBX2_CWE_OBX5_NM.txt
    }

    @Test
    fun testPIDSocialSecurityElr(){
        //use COVID-19_With_SSN_PID19.txt
    }
    @Test
    fun testPIDDateOfBirthElr(){
        //use COVID-19_PID_DateOfBirth.txt

    }
    @Test
    fun testPIDNameAndAddressCElR(){
        //COVID19_PID_Segment_With_Patient_Name_And_Address.txt
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