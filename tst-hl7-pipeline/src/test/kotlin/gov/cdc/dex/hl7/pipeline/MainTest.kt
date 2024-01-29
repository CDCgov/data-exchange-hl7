package gov.cdc.dex.hl7.pipeline

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import kotlin.test.assertEquals

import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException

class MainTest {
    private fun getTheNewPayload(fileEnding: String): String? {
        val directoryWithPayloads = File(Constants.NEW_PAYLOADS_DIRECTORY)
        return directoryWithPayloads.listFiles { payload ->
            payload.isFile && payload.name.endsWith(fileEnding)
        }?.find { it.isFile }?.absolutePath
    }
    private fun mapJsonToJsonNode(jsonString: File): JsonNode {
        val jsonMapper = jacksonObjectMapper()
        return jsonMapper.readTree(jsonString)
    }
    private fun getFieldDescriptionForPath(path: String, payloadName: String): String {
        try {
            val newPayload = getTheNewPayload(payloadName)?.let { File(it) }

            if (newPayload != null && newPayload.exists()) {
                val jsonNewPayload: JsonNode = mapJsonToJsonNode(newPayload)
                val structureValidatorReportInNewPayload =
                    jsonNewPayload[Constants.METADATA][Constants.PROCESSES][2][Constants.REPORT][Constants.ENTRIES][Constants.STRUCTURE_VALIDATOR]
                for (structureError in structureValidatorReportInNewPayload) {

                    if (structureError[Constants.PATH].toString() == "\"$path\"") {
                        return structureError[Constants.DESCRIPTION].toString()
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        return "$path not found in the payload: $payloadName"
    }


    @Test
    fun testMessageInfo() {
        //choose  any payload

    }

    @Test
    fun testSummaryWithProblemAttribute() {
        //read any error payload
    }

    @Test
    fun phlipFluPIDSegmentFailure() {
        /*
       Compares redactor and structure validator reports
        */
        val newPayload = getTheNewPayload("PHLIP_FLU_2.5.1_PID5_ERROR.json")?.let { File(it) }

        val verifiedPayload = File("src/test/resources/verified-payloads/PHLIP_FLU_2.5.1_PID5_ERROR.json")

        if (newPayload != null) {
            if (newPayload.exists() && verifiedPayload.exists()) {
                val jsonNewPayload: JsonNode = mapJsonToJsonNode(newPayload)
                val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)

                val redactorReportInNewPayload = jsonNewPayload["metadata"]["processes"][1]["report"]
                val redactorReportInVerifiedPayload = jsonVerifiedPayload["metadata"]["processes"][1]["report"]

                val structureValidatorReportInNewPayload = jsonNewPayload["metadata"]["processes"][2]["report"]
                val structureValidatorReportInVerifiedPayload =
                    jsonVerifiedPayload["metadata"]["processes"][2]["report"]

                assertEquals(
                    redactorReportInNewPayload,
                    redactorReportInVerifiedPayload,
                    "The Redactor Reports DO NOT Match")
                assertEquals(
                    structureValidatorReportInNewPayload,
                    structureValidatorReportInVerifiedPayload,
                    "The Structure Validator Reports DO NOT Match")

            }
        }
    }
        @Test
        fun phlipFluMissingMSH3() {
            val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH3,Constants.PHLIP_FLU_NO_MSH3)

            val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_NO_MSH3}")
            val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
            val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

            assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "Incorrect error message for missing MSH-3.")
        }



       @Test
       fun phlipFluMissingMSH4() {
           val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH4, Constants.PHLIP_FLU_NO_MSH4)

           val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_NO_MSH4}")
           val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
           val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

           assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "Incorrect error message for missing MSH-3.")
       }
    @Test
    fun phlipFluMissingMSH5() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH5,Constants.PHLIP_FLU_NO_MSH5)

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_NO_MSH5}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Component MSH-5.2 (Universal ID) is missing")
    }
    @Test
    fun phlipFluMissingMSH6() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH6,Constants.PHLIP_FLU_NO_MSH6)

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_NO_MSH6}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-6 (Receiving Facility) is missing")
    }
    @Test
    fun phlipFluMissingMSH7() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH7,Constants.PHLIP_FLU_NO_MSH7)

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_NO_MSH7}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-7 (Date/Time Of Message) is missing.")
    }

    @Test
    fun phlipFluMissingMSH9() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH9,Constants.PHLIP_FLU_NO_MSH9)

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_NO_MSH9}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-9 (Message Type) is missing")
    }
    @Test
    fun phlipFluMissingMSH10() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH10,Constants.PHLIP_FLU_NO_MSH10)

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_NO_MSH10}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-10 (Message Control ID) is missing.")
    }
    @Test
    fun phlipFluMissingMSH11() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH11,Constants.PHLIP_FLU_NO_MSH11)

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_NO_MSH10}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-11 (Processing ID) is missing")
    }
    @Disabled
    @Test
    fun phlipFluMissingMSH12() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath("",Constants.PHLIP_FLU_NO_MSH12)

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_NO_MSH12}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "HL7-2801")
    }
    @Test
    fun phlipFluMissingMSH21() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH21,Constants.PHLIP_FLU_NO_MSH21)

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_NO_MSH21}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-21 (Message Profile Identifier) is missing")
    }
    @Disabled
    @Test
    fun phlipFluNoProfileIdentifier() {
        /*
        if there are no components in MSH21, payload does include error.
        if MSH21.2 is missing, which for PHLIP FLU is a profile identifier, the error is not detected.
        Current design depends on metadata set by user to identify the profile for validation.
        Adding it as placeholder, and disabling the test case may revisit later.
         */
    }
    @Test
    fun phlipFluValidMessage() {
        /*
        This test will check all required fields in all required segments and compare against verified payload
         */
        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_DIRECTORY}/${Constants.PHLIP_FLU_VALID_MESSAGE}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)

        val newPayload = getTheNewPayload(Constants.PHLIP_FLU_VALID_MESSAGE)?.let { File(it) }


        if (newPayload != null && newPayload.exists()) {
            val jsonNewPayload: JsonNode = mapJsonToJsonNode(newPayload)
            
            val MSHfieldsToCompare = setOf(Constants.FIELD_SEPARATOR,Constants.ENCODING_CHARACTERS, Constants.SENDING_APPLICATION,
                Constants.SENDING_FACILITY,Constants.RECEIVING_APPLICATION,Constants.RECEIVING_FACILITY,Constants.DATE_TIME_OF_MESSAGE,
                Constants.MESSAGE_TYPE,Constants.MESSAGE_CONTROL_ID,Constants.PROCESSING_ID,Constants.VERSION_ID, Constants.ACCEPT_ACKNOWLEDGEMENT_TYPE,
                Constants.APPLICATION_ACKNOWLEDGEMENT_TYPE,Constants.COUNTRY_CODE,Constants.MESSAGE_PROFILE_IDENTIFIER)

            for (field in MSHfieldsToCompare) {
                val fieldFromNewPayload = jsonNewPayload[Constants.METADATA][Constants.PROCESSES][3][Constants.REPORT][Constants.MSH][field]
                val fieldFromVerifiedPayload = jsonVerifiedPayload[Constants.REPORT][Constants.MSH][field]
                assertEquals(
                    fieldFromNewPayload,
                    fieldFromVerifiedPayload,
                    "Required fields for MSH $field do not match!"
                )
            }
            
            val SFTfieldsToCompare = setOf(Constants.SOFTWARE_VENDOR_ORGANIZATION,Constants.SOFTWARE_CERTIFIED_VERSION_OR_RELEASE_NUMBER,
                Constants.SOFTWARE_PRODUCT_NAME, Constants.SOFTWARE_BINARY_ID, Constants.SOFTWARE_INSTALL_DATE)

            for (field in SFTfieldsToCompare) {
                val fieldFromNewPayload = jsonNewPayload[Constants.METADATA][Constants.PROCESSES][3][Constants.REPORT][Constants.MSH][Constants.CHILDREN][0][Constants.SFT][field]
                val fieldFromVerifiedPayload = jsonVerifiedPayload[Constants.REPORT][Constants.MSH][Constants.CHILDREN][0][Constants.SFT][field]
                assertEquals(
                    fieldFromNewPayload,
                    fieldFromVerifiedPayload,
                    "Required fields for SFT $field do not match!"
                )
            }
            
            val PIDfieldsToCompare = setOf(Constants.SET_ID, Constants.PATIENT_IDENTIFIER_LIST, Constants.PATIENT_NAME,
                Constants.PATIENT_MOTHER_MAIDEN_NAME, Constants.PATIENT_BIRTH_DATE_TIME, Constants.PATIENT_SEX,
                Constants.PATIENT_SEX, Constants.PATIENT_RACE, Constants.PATIENT_ADDRESS, Constants.PATIENT_PHONE_NUMBER,
                Constants.PATIENT_BUSINESS_PHONE_NUMBER, Constants.PATIENT_ETHNIC_GROUP, Constants.PATIENT_DEATH_DATE_TIME,
                Constants.PATIENT_DEATH_INDICATOR, Constants.PATIENT_LAST_DEMOGRAPHIC_INFO_DATE_TIME_UPDATE,
                Constants.PATIENT_LAST_UPDATE_FACILITY, Constants.PATIENT_SPECIES_CODE, Constants.PATIENT_CLASS,
                Constants.PATIENT_ADMISSION_TYPE, Constants.PATIENT_ADMISSION_DATE_TIME, Constants.PATIENT_DISCHARGE_DATE_TIME)

            val ORCfieldsToCompare = setOf(Constants.ORDER_CONTROL, Constants.PLACER_ORDER_NUMBER, Constants.FILLER_ORDER_NUMBER,
                Constants.PLACER_GROUP_NUMBER, Constants.ORDERING_PROVIDER, Constants.ORDERING_FACILITY_NAME, Constants.ORDERING_FACILITY_ADDRESS,
                Constants.ORDERING_FACILITY_PHONE_NUMBER, Constants.ORDERING_PROVIDER_ADDRESS, Constants.ORDERING_PROVIDER_ADDRESS,
                )
            val OBRfieldsToCompare = setOf(Constants.SET_ID, Constants.PLACER_ORDER_NUMBER, Constants.FILLER_ORDER_NUMBER,
                Constants.UNIVERSAL_SERVICE_IDENTIFIER, Constants.OBSERVATION_DATE_TIME, Constants.OBSERVATION_DATE_TIME_END,
                Constants.RELEVANT_CLINICAL_INFORMATION, Constants.ORDERING_PROVIDER, Constants.RESULT_REPORT_DATE_TIME,
                Constants.RESULT_STATUS, Constants.PARENT_RESULT, Constants.PARENT_ID, Constants.REASON_FOR_STUDY,Constants.PRINCIPAL_RESULT_INTERPRETER,
                )
            val OBXfieldsToCompare = setOf(Constants.SET_ID, Constants.VALUE_DATA_TYPE, Constants.OBSERVATION_IDENTIFIER,
                Constants.OBSERVATION_SUB_ID, Constants.OBSERVATION_VALUE,Constants.UNITS_OF_MEASURE_FOR_DATA_TYPE_SN,
                Constants.REFERENCE_RANGE, Constants.ABNORMAL_FLAGS, Constants.OBSERVATION_RESULT_STATUS, Constants.OBSERVATION_METHOD,
                Constants.DATE_TIME_ANALYSIS, Constants.PERFORMING_ORGANIZATION_NAME, Constants.PERFORMING_ORGANIZATION_ADDRESS, Constants.PERFORMING_ORGANIZATION_MEDICAL_DIRECTOR,
                )
            val SPMfieldsToCompare = setOf(Constants.SET_ID, Constants.SPECIMEN_ID,Constants.SPECIMEN_TYPE,
                Constants.SPECIMEN_TYPE_MODIFIER, Constants.SPECIMEN_ADDITIVES, Constants.SPECIMEN_COLLECTION_METHOD,
                Constants.SPECIMEN_SOURCE_SITE, Constants.SPECIMEN_SOURCE_SITE_MODIFIER, Constants.SPECIMEN_ROLE,
                Constants.SPECIMEN_COLLECTION_AMOUNT, Constants.SPECIMEN_COLLECTION_DATE_TIME,
                Constants.SPECIMEN_RECEIVED_DATE_TIME, Constants.SPECIMEN_REJECT_REASON,
                )
        }



    }

    @Disabled
    @Test
       fun phlipFluCardinalityForMSH2() {
           // HL7-2802 -- revisit once resolved
       }

       @Test
       fun testBatchMessageCountAgainstActualMessagesInBatch() {
           //should fail if counts do not match
       }

       @Test
       fun testForEmptyBatches() {
       }

       @Test
       fun testForInvalidBatchDueToMissingFHSSegment() {
       }

       @Test
       fun testForInvalidBatchDueToMissingBHSSegment() {
       }

       @Test
       fun testForInvalidBatchDueToMissingBTSSegment() {
       }

       @Test
       fun testForInvalidBatchCountInBTSSegment() {
           //should fail
       }

       @Test
       fun testPhlipFluWithDataTypeNM() {
           //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
       }

       @Test
       fun testPhlipFluWithDataTypeTS() {
           //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
       }

       @Test
       fun testPhlipFluWithDataTypeTM() {
           //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
       }

       @Test
       fun testPhlipFluWithDataTypeDT() {
           //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
       }

       @Test
       fun testPhlipFluWithDataTypeCWE() {
           //use PHLIP_FLU_DataType_CWE.txt
       }

       @Test
       fun testPhlipFluWithDataTypeFT() {
           //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
       }

       @Test
       fun testPhlipFluWithDataTypeTX() {
           //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
       }

       @Test
       fun testPhlipFluWithDataTypeSN() {
           //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
       }

       @Test
       fun testPhlipFluWithDataTypeCX() {
           //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
       }

       @Test
       fun testPhlipFluWithDataTypeCE() {
           //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt
       }

       @Test
       fun testPhlipFluWithDataTypeED() {
           //PHLIP_FLU_OBX2_SN_CX_CE_NM_ED_TX_TS_TM_DT_FT.txt

       }

       @Test
       fun testPhlipFluWithDataTypeRP() {
           // need to find out
       }

       @Test
       fun testVPDWithDataTypeNM() {
           // use PHLIP_VPD_VALID.txt OBX3

       }

       @Test
       fun testVPDWithDataTypeSN() {
           //useuse PHLIP_VPD_VALID OBX56, 57,58

       }

       @Test
       fun testVPDWithDataTypeTS() {
           //use PHLIP_VPD_VALID OBX35
       }

       @Test
       fun testVPDWithDataTypeTM() {
           //use PHLIP_VPD_VALID  add TM
       }

       @Test
       fun testVPDWithDataTypeDT() {
           //use PHLIP_VPD_VALID OBX 37, 38, 39
       }

       @Test
       fun testVPDWithDataTypeCWE() {
           //use PHLIP_VPD_VALID OBX 40, 41, 42
       }

       @Test
       fun testVPDWithDataTypeFT() {
           //PHLIP_VPD_VALID_DataType_FT.txt OBX175
       }

       @Test
       fun testVPDWithDataTypeTX() {
           //use PHLIP_VPD_VALID OBX 102
       }

       @Test
       fun testVPDWithDataTypeED() {
           //PHLIP_VPD_DataType_ED.txt
       }

       @Test
       fun testVPDWithDataTypeRP() {
           // need to find out
       }

       @Test
       fun testRequiredFieldsPIDCase() {
           //PHLIP_Salm_PID_Required_Fields_Case.txt
       }

       @Test
       fun testOBX2HasRightDatatypeInOBX5Case() {
           // use PHLIP_DataType_DT_CASE.txt
       }

       @Test
       fun testOBX2HasIncorrectDatatypeInOBX5Case() {
           // PHLIP_OBX2_CWE_OBX5_ST.txt OBR1 OBX1
       }

       @Test
       fun testOBX1ValueShouldBeUniqueCELR() {
           // use COVID-19_OBX1_Uniqueness_Test.txt

       }

       @Test
       fun testOBX1ValueShouldBeSequentialCELR() {
           //use COVID-19_OBX_Sequentional_Test.txt

       }

       @Test
       fun testSendingAndReceivingApplicationsCASE() {
           //use PHLIP_FLU_Receiving_Sending_Applications.txt
       }

       @Test
       fun testPIDSocialSecurityCase() {
           //PHLIP_FLU_PID_19.txt
       }

       @Test
       fun testPIDDateOfBirthCase() {
           // use PHLIP_FLU_PID_7_DateTimeOfBirth.txt
       }

       @Test
       fun testPIDNameAndAddressCase() {
           //use FDD_LIST_PID_Name_Address_CASE.txt
       }

       @Test
       fun testUniqueOBXWithSameOBX3AndNullOBX4Case() {
           //failure message
       }

       @Test
       fun testUniqueOBXWithSameOBX3AndDifferentOBX4Case() {
           //failure
       }

       @Test
       fun testWithMissingMessageProfileIdentifierValueCase() {
           //failure
       }

       @Test
       fun testRequiredFieldsPIDElr() {
           //use COVID-19_PID_Required_Fields.txt
       }

       @Test
       fun testOBX2HasRightDatatypeInOBX5Elr() {
           //use COVID-19_OBX2CWE_OBX5_CWE.txt #OBX|6
       }

       @Test
       fun testOBX2HasIncorrectDatatypeInOBX5Elr() {
           //COVID-19_OBX2_CWE_OBX5_NM.txt
       }

       @Test
       fun testPIDSocialSecurityElr() {
           //use COVID-19_With_SSN_PID19.txt
       }

       @Test
       fun testPIDDateOfBirthElr() {
           //use COVID-19_PID_DateOfBirth.txt

       }

       @Test
       fun testPIDNameAndAddressCElR() {
           //COVID19_PID_Segment_With_Patient_Name_And_Address.txt
       }

       @Test
       fun testUniqueOBXWithSameOBX3AndNullOBX4Elr() {
           //failure message
       }

       @Test
       fun testUniqueOBXWithSameOBX3AndDifferentOBX4Elr() {
           //failure
       }



    companion object {
        @JvmStatic
        @BeforeAll
        fun sendMessagesThroughPipeline() {
            val pipelineTest = PipelineTest()
            pipelineTest.dropMessagesToABlobStorage()
        }


        @JvmStatic
        @AfterAll
        fun cleanup() {
            val newPayloadsFolder = File("src/test/resources/new-payloads/")
            if (newPayloadsFolder.exists() && newPayloadsFolder.isDirectory) {
                newPayloadsFolder.listFiles()?.forEach { payload->
                    payload.delete()
                }
            }

        }





    }




}