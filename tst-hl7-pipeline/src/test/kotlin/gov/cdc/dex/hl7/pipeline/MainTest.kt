package gov.cdc.dex.hl7.pipeline

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import kotlin.test.assertEquals

import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileNotFoundException

class MainTest {
    private fun getTheNewPayload(fileEnding: String): String? {
        val directoryWithPayloads = File(Constants.NEW_PAYLOADS_PATH)
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
        val newPayload = getTheNewPayload(Constants.PHLIP_FLU_PID5_ERROR.replace("txt","json"))?.let { File(it) }

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_PID5_ERROR.replace("txt","json")}")

        if (newPayload != null) {
            if (newPayload.exists() && verifiedPayload.exists()) {
                val jsonNewPayload: JsonNode = mapJsonToJsonNode(newPayload)
                val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)

                val redactorReportInNewPayload = jsonNewPayload[Constants.METADATA][Constants.PROCESSES][1][Constants.REPORT]
                val redactorReportInVerifiedPayload = jsonVerifiedPayload[Constants.METADATA][Constants.PROCESSES][1][Constants.REPORT]

                val structureValidatorReportInNewPayload = jsonNewPayload[Constants.METADATA][Constants.PROCESSES][2][Constants.REPORT]
                val structureValidatorReportInVerifiedPayload =
                    jsonVerifiedPayload[Constants.METADATA][Constants.PROCESSES][2][Constants.REPORT]

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
            val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH3,Constants.PHLIP_FLU_NO_MSH3.replace("txt","json"))

            val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_NO_MSH3.replace("txt","json")}")
            val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
            val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

            assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "Incorrect error message for missing MSH-3.")
        }



       @Test
       fun phlipFluMissingMSH4() {
           val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH4, Constants.PHLIP_FLU_NO_MSH4.replace("txt","json"))
           val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_NO_MSH4.replace("txt","json")}")
           val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
           val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

           assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "Incorrect error message for missing MSH-3.")
       }
    @Test
    fun phlipFluMissingMSH5() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH5,Constants.PHLIP_FLU_NO_MSH5.replace("txt","json"))

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_NO_MSH5.replace("txt","json")}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Component MSH-5.2 (Universal ID) is missing")
    }
    @Test
    fun phlipFluMissingMSH6() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH6,Constants.PHLIP_FLU_NO_MSH6.replace("txt","json"))

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_NO_MSH6.replace("txt","json")}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-6 (Receiving Facility) is missing")
    }
    @Test
    fun phlipFluMissingMSH7() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH7,Constants.PHLIP_FLU_NO_MSH7.replace("txt","json"))

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_NO_MSH7.replace("txt","json")}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-7 (Date/Time Of Message) is missing.")
    }

    @Test
    fun phlipFluMissingMSH9() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH9,Constants.PHLIP_FLU_NO_MSH9.replace("txt","json"))

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_NO_MSH9.replace("txt","json")}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-9 (Message Type) is missing")
    }
    @Test
    fun phlipFluMissingMSH10() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH10,Constants.PHLIP_FLU_NO_MSH10.replace("txt","json"))

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_NO_MSH10.replace("txt","json")}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-10 (Message Control ID) is missing.")
    }
    @Test
    fun phlipFluMissingMSH11() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH11,Constants.PHLIP_FLU_NO_MSH11.replace("txt","json"))
        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_NO_MSH11.replace("txt","json")}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()
        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "The required Field MSH-11 (Processing ID) is missing")
    }
    @Disabled
    @Test
    fun phlipFluMissingMSH12() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath("",Constants.PHLIP_FLU_NO_MSH12.replace("txt","json"))

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_NO_MSH12.replace("txt","json")}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)
        val errorDescriptionInVerifiedPayload = jsonVerifiedPayload[Constants.DESCRIPTION].toString()

        assertEquals(errorDescriptionInNewPayload, errorDescriptionInVerifiedPayload, "HL7-2801")
    }
    @Test
    fun phlipFluMissingMSH21() {
        val errorDescriptionInNewPayload = getFieldDescriptionForPath(Constants.MSH21,Constants.PHLIP_FLU_NO_MSH21.replace("txt","json"))

        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_NO_MSH21.replace("txt","json")}")
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
        val verifiedPayload = File("${Constants.VERIFIED_PAYLOADS_PATH}/${Constants.PHLIP_FLU_VALID_MESSAGE.replace("txt","json")}")
        val jsonVerifiedPayload: JsonNode = mapJsonToJsonNode(verifiedPayload)

        val newPayload = getTheNewPayload(Constants.PHLIP_FLU_VALID_MESSAGE.replace("txt","json"))?.let { File(it) }


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
    @Test
    fun phlipFluDifferentDataTypesInOBX2() {
       // use PHLIP_FLU_2.5.1_VALID_DT_NM_CE_CX_SN_ST_TS_TX.txt
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
       fun phlipFluOBX2HasIncorrectDatatypeInOBX5() {
           // PHLIP_OBX2_CWE_OBX5_ST.txt OBR1 OBX1
       }

       @Test
       fun testOBX1ValueShouldBeUniqueCELR() {
           // use COVID-19_OBX1_Uniqueness_Test.txt

       }



       @Test
       fun phlipFluPIDSsn() {
           //PHLIP_FLU_PID_19.txt
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

        /*
        @JvmStatic
        @AfterAll
        fun cleanup() {
            val newPayloadsFolder = File("src/test/resources/new-payloads/")
            if (newPayloadsFolder.exists() && newPayloadsFolder.isDirectory) {
                newPayloadsFolder.listFiles()?.forEach { payload->
                    payload.delete()
                }
            }
         }*/






    }




}