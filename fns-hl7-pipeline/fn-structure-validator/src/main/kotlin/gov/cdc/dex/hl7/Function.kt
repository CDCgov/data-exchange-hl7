package gov.cdc.dex.hl7

import com.azure.messaging.eventhubs.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.hl7.model.StructureValidatorStageMetadata
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.InvalidMessageException
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.HL7StaticParser
import gov.cdc.nist.validator.NistReport
import org.slf4j.LoggerFactory
import java.util.*


class ValidatorFunction {
    companion object {
        private const val DEFAULT_SPEC_PROFILE = "MSH-12"
        const val PROCESS_STATUS_OK = "SUCCESS"
        const val PROCESS_STATUS_EXCEPTION = "FAILURE"
        private const val NIST_VALID_MESSAGE = "VALID_MESSAGE"
        private const val NIST_INVALID_MESSAGE = "STRUCTURE_ERRORS"
        private const val HL7_MSH = "MSH|"
        private const val HL7_SUBDELIMITERS = "^~\\&"
        private var logger = LoggerFactory.getLogger(ValidatorFunction::class.java.simpleName)
        val gson = GsonBuilder().serializeNulls().create()!!

        val fnConfig = FunctionConfig()
    }

    /**
     * This function will be invoked when an event is received from Event Hub.
     */
    @FunctionName("NIST-VALIDATOR")
    fun eventHubProcessor(
        @EventHubTrigger(
            name = "message",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%",
            cardinality = Cardinality.MANY
        ) message: List<String?>,
        @BindingName("SystemPropertiesArray") eventHubMD: List<EventHubMetadata>
    ): JsonObject {
        logger.info("Function triggered. Version: ${fnConfig.functionVersion}")
        val outList = mutableListOf<String>()
        try {
            message.forEachIndexed { msgNumber: Int, singleMessage: String? ->
                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                val startTime = Date().toIsoString()
                var report = NistReport()
                var messageUUID = ""
                try {
                    val hl7Content = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
                    val filePath =
                        JsonHelper.getValueFromJson("routing_metadata.ingested_file_path", inputEvent).asString
                    messageUUID = JsonHelper.getValueFromJson("message_metadata.message_uuid", inputEvent).asString
                    val dataStream =
                        JsonHelper.getValueFromJson("routing_metadata.data_stream_id", inputEvent).asString.uppercase()

                    logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath, messageType: $dataStream")

                    // generate NIST report
                    val profileInfo = getProfileNameAndPaths(hl7Content, dataStream)
                    report = validateMessage(hl7Content, dataStream, profileInfo)
                    // prepare EventHub payload
                    val stageMD = StructureValidatorStageMetadata(
                        PROCESS_STATUS_OK,
                        report,
                        eventHubMD[msgNumber],
                        listOf(profileInfo.first)
                    )

                    stageMD.startProcessTime = startTime
                    stageMD.endProcessTime = Date().toIsoString()

                    inputEvent.add("stage", stageMD.toJsonElement())
                    //Update Summary element.
                    val summary = SummaryInfo(report.status ?: "Unknown")
                    if (NIST_VALID_MESSAGE != report.status) {
                        summary.problem =
                            Problem(
                                StructureValidatorStageMetadata.VALIDATOR_PROCESS,
                                "Message failed Structure Validation"
                            )
                    }
                    inputEvent.add("summary", summary.toJsonElement())

                    outList.add(gson.toJson(inputEvent))
                    logger.info("Processed structure validation for messageUUID: $messageUUID, filePath: $filePath, report.status: ${report.status}")

                } catch (e: Exception) {
                    //TODO::  - update retry counts
                    logger.error("Unable to process Message due to exception: ${e.message}")
                    val stageMD = StructureValidatorStageMetadata(
                        PROCESS_STATUS_EXCEPTION,
                        report,
                        eventHubMD[msgNumber],
                        listOf()
                    )
                    stageMD.startProcessTime = startTime
                    stageMD.endProcessTime = Date().toIsoString()
                    inputEvent.add("stage", stageMD.toJsonElement())
                    val problem = Problem(StructureValidatorStageMetadata.VALIDATOR_PROCESS, "Error: ${e.message}")
                    val summary = SummaryInfo(NIST_INVALID_MESSAGE, problem)
                    inputEvent.add("summary", summary.toJsonElement())
                    outList.add(gson.toJson(inputEvent))
                    logger.info("Sent Message to Error Event Hub for Message Id $messageUUID")
                }
            } // foreachIndexed
        } catch (ex: Exception) {
            logger.error("An unexpected error occurred: ${ex.message}")
        }
        try {
            fnConfig.evHubSender.send(outList)
        } catch (e: Exception) {
            logger.error("Unable to send to event hub ${fnConfig.evHubSendName}: ${e.message}")
        }
        return JsonObject()

    } //.eventHubProcessor


    fun getProfileNameAndPaths(hl7Content: String, dataStream: String): Pair<String, List<String>> {
        validateHL7Delimiters(hl7Content)
        val dataStreamName = dataStream.uppercase().trim()
        val profileList = fnConfig.profileConfig.profileIdentifiers.filter {
            it.dataStreamId.uppercase().trim() == dataStreamName
        }
        // if the route is not specified in the config file, assume the default of MSH-12
        val profileIdPaths = if (profileList.isNotEmpty()) {
            profileList[0].identifierPaths
        } else listOf(DEFAULT_SPEC_PROFILE)

        val prefix = if (dataStreamName.isNotEmpty()) "$dataStreamName-" else ""
        val profileName = try {
            prefix + profileIdPaths.map { path ->
                HL7StaticParser.getFirstValue(hl7Content, path).get().uppercase()
            }.reduce { acc, map -> "$acc-$map" }
        } catch (e: NoSuchElementException) {
            throw InvalidMessageException(
                "Unable to load validation profile: " +
                        "One or more values in the profile path(s)" +
                        " ${profileIdPaths.joinToString()} are missing."
            )
        }
        return Pair(profileName, profileIdPaths)

    }

    private fun validateMessage(hl7Message: String, dataStream: String): NistReport {
        val profileNameAndPaths = getProfileNameAndPaths(hl7Message, dataStream)
        return validateMessage(hl7Message, dataStream, profileNameAndPaths)
    }

    private fun validateMessage(
        hl7Message: String,
        dataStream: String,
        profileInfo: Pair<String, List<String>>
    ): NistReport {
        val profileName = profileInfo.first
        val profilePaths = profileInfo.second
        val nistValidator = fnConfig.getNistValidator(profileName)
        if (nistValidator != null) {
            val report = nistValidator.validate(hl7Message)
            report.status = if (!report.status.isNullOrEmpty() && ("ERROR" !in report.status + "")) {
                report.status + ""
            } else if ("ERROR" in report.status + "") {
                NIST_INVALID_MESSAGE
            } else {
                "UNKNOWN"
            }
            return report
        } else {
            throw InvalidMessageException(
                "Unable to find validation profile named $profileName."
                        + " Either the data stream ID '$dataStream' or the data in HL7 path(s) " +
                        "'${profilePaths.joinToString()}' is invalid."
            )
        }
    }


    private fun validateHL7Delimiters(hl7Message: String) {
        val msg = hl7Message.trim()
        val mshPos = msg.indexOf(HL7_MSH)
        if (mshPos > -1) {
            val delimiters =
                msg.substring(mshPos + HL7_MSH.length, mshPos + (HL7_MSH.length + HL7_SUBDELIMITERS.length))
            if (delimiters != HL7_SUBDELIMITERS) {
                throw InvalidMessageException("Invalid delimiters found in message header: found '$delimiters', expected '$HL7_SUBDELIMITERS'")
            }
        } else {
            throw InvalidMessageException("Unable to locate message header sequence '$HL7_MSH'")
        }
    }

    @FunctionName("structure")
    fun invoke(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>
    ): HttpResponseMessage {

        val hl7Message = try {
            request.body?.get().toString()
        } catch (e: NoSuchElementException) {
            return buildHttpResponse(
                "No body was found. Please send an HL7 v.2.x message in the body of the request.",
                HttpStatus.BAD_REQUEST,
                request
            )
        }
        // notify user when required header values are missing/empty
        val dataStream = if (!request.headers["x-tp-data_stream_id"].isNullOrEmpty()) {
            request.headers["x-tp-data_stream_id"].toString()
        } else {
            return buildHttpResponse(
                "BAD REQUEST: You must specify a data stream ID" +
                        " in the HTTP header as 'x-tp-data_stream_id'. " +
                        "Please correct the HTTP header and try again.",
                HttpStatus.BAD_REQUEST,
                request
            )
        }
        if (dataStream.isNotEmpty()) {
            logger.info("Message data stream ID received: $dataStream")
        }
        return try {
            val report = validateMessage(
                hl7Message, dataStream
            )
            logger.info("Validation report created OK")
            buildHttpResponse(gson.toJson(report), HttpStatus.OK, request)
        } catch (e: Exception) {
            buildHttpResponse(
                "${e.message}",
                HttpStatus.BAD_REQUEST,
                request
            )
        }
    }

    private fun buildHttpResponse(
        message: String,
        status: HttpStatus,
        request: HttpRequestMessage<Optional<String>>
    ): HttpResponseMessage {
        // need to be able to send plain text exception message that is not formatted as json
        val contentType = if (status == HttpStatus.OK) {
            "application/json"
        } else {
            logger.error(message)
            "text/plain"
        }
        return request
            .createResponseBuilder(status)
            .header("Content-Type", contentType)
            .body(message)
            .build()
    }
}