package gov.cdc.dex.hl7.pipeline

import java.io.File
import java.io.FileNotFoundException
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
class Utility {
    fun buildMetadata(uniqueTimeStamp: String): Map<String, MutableMap<String, out String?>> {
        return mapOf(
            Constants.PHLIP_FLU_NO_MSH3 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH3}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH4 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH4}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH5 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH5}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH6 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH6}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH7 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH7}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH9 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH9}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH10 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH10}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH11 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH11}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH12 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH12}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_MSH21 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_MSH21}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_NO_PROFILE_IDENTIFIER to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_NO_PROFILE_IDENTIFIER}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_VALID_MESSAGE to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_VALID_MESSAGE}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_VALID_MESSAGE_WITH_PV1 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_VALID_MESSAGE_WITH_PV1}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_PID5_ERROR to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_PID5_ERROR}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            ),
            Constants.PHLIP_FLU_WITH_PID22 to mutableMapOf<String, String?>(
                Constants.MESSAGE_TYPE_STR to Constants.MESSAGE_TYPE_ELR,
                Constants.ORIGINAL_FILE_NAME to "$uniqueTimeStamp-${Constants.PHLIP_FLU_WITH_PID22}",
                Constants.ROUTE to Constants.PHLIP_FLU,
                Constants.REPORTING_JURISDICTION to Constants.JURISDICTION
            )

        )
    }

    fun getCurrentDateTimeWithSeconds(): String {
        return try {
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern(Constants.DATE_TIME_PATTERN)
            currentDateTime.format(formatter)
        } catch (e: DateTimeParseException) {
            println("${e.message}")
            "DEX::tst-hl7-pipeline Unable to retrieve date and time"
        }

    }

    fun addPayloadToTestResources(payloadAsJson: String, originalFileName: String) {
        val encodedOriginalFileName =
            URLEncoder.encode(originalFileName.replace(".txt", ".json"), "UTF-8").replace("%20", "_")
        val testResourcesDirectory = "${Constants.NEW_PAYLOADS_PATH}/$encodedOriginalFileName"
        try {
            val jsonFileWithPayload = File(testResourcesDirectory)

            if (!jsonFileWithPayload.exists()) {
                jsonFileWithPayload.writeText(payloadAsJson)

            }
        } catch (e: FileNotFoundException) {
            PipelineTest.logger.error("DEX::tst-hl7-pipeline Error occurred while copying payload to $testResourcesDirectory - exception details ${e.printStackTrace()}")
        }
    }
}