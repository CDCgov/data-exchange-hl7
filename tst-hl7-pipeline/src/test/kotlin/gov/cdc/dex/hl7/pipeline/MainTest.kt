package gov.cdc.dex.hl7.pipeline

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals

import org.junit.jupiter.api.Test
import java.io.File

class MainTest{
    /*
    TO BE IMPLEMENTED
     */
    @Test
    fun verifyPayloadsExist(){

    }
    @Test
    fun verifyMessageInfo(){

    }
    @Test
    fun verifySummaryWithProblemAttribute(){
    }

    @Test
    fun verifyRequiredFieldsPIDCase(){

    }
    @Test
    fun verifyRequiredFieldsPIDElr(){

    }
    @Test
    fun verifyOBX2HasRightDatatypeInOBX5Case(){

    }
    @Test
    fun verifyOBX2HasRightDatatypeInOBX5Elr(){

    }
    @Test
    fun requiredFieldsMSHCase(){
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
}