package gov.cdc.dex.hl7

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import com.azure.messaging.eventhubs.*
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.metadata.HL7MessageType
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.hl7.model.StructureValidatorProcessMetadata
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
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
        @BindingName("SystemPropertiesArray") eventHubMD:List<EventHubMetadata>
    ): JsonObject {
        logger.info("Function triggered. Version: ${fnConfig.functionVersion}")
        val outList = mutableListOf<String>()
        try {
            message.forEachIndexed { msgNumber: Int, singleMessage: String? ->
                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                val startTime = Date().toIsoString()
                var report = NistReport()
                var metadata = JsonObject()
                var messageUUID = ""
                try {
                    val hl7Content = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
                    metadata = JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject
                    val filePath = JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                    messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString
                    val messageType = JsonHelper.getValueFromJson("message_info.type", inputEvent).asString
                    val route = if (messageType.uppercase() == HL7MessageType.CASE.toString()) {
                        ""
                    } else {
                        val routeJson = JsonHelper.getValueFromJson("message_info.route", inputEvent)
                        if (routeJson is JsonPrimitive) routeJson.asString.uppercase() else ""
                    }

                    logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath, messageType: $messageType")
                    //Main FN Logic
                    report = validateMessage(hl7Content, HL7MessageType.valueOf(messageType), route)
                    //preparing EventHub payload:
                    val processMD = StructureValidatorProcessMetadata(
                        PROCESS_STATUS_OK,
                        report,
                        eventHubMD[msgNumber],
                        listOf(getProfileNameAndPaths(hl7Content, route).first)
                    )

                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()

                    metadata.addArrayElement("processes", processMD)
                    //Update Summary element.
                    val summary = SummaryInfo(report.status ?: "Unknown")
                    if (NIST_VALID_MESSAGE != report.status) {
                        summary.problem =
                            Problem(
                                StructureValidatorProcessMetadata.VALIDATOR_PROCESS,
                                "Message failed Structure Validation"
                            )
                    }
                    inputEvent.add("summary", JsonParser.parseString(gson.toJson(summary)))

                    outList.add(gson.toJson(inputEvent))
                    logger.info("Processed structure validation for messageUUID: $messageUUID, filePath: $filePath, report.status: ${report.status}")

                } catch (e: Exception) {
                    //TODO::  - update retry counts
                    logger.error("Unable to process Message due to exception: ${e.message}")
                    val processMD = StructureValidatorProcessMetadata(
                        PROCESS_STATUS_EXCEPTION,
                        report,
                        eventHubMD[msgNumber],
                        listOf()
                    )
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()
                    metadata.addArrayElement("processes", processMD)
                    val problem = Problem(StructureValidatorProcessMetadata.VALIDATOR_PROCESS, "Error: ${e.message}")
                    val summary = SummaryInfo(NIST_INVALID_MESSAGE, problem)

                    logger.error("metadata in exception: $metadata")
                    logger.info("inputEvent in exception:$inputEvent")

                    inputEvent.add("summary", summary.toJsonElement())
                    outList.add(gson.toJson(inputEvent))
                    logger.info("Sent Message to Error Event Hub for Message Id $messageUUID")
                }
            } // foreachIndexed
        } catch (ex: Exception) {
            logger.error("An unexpected error occurred: ${ex.message}")
        }
        try {
            fnConfig.evHubSender.send(fnConfig.evHubSendName, outList)
        } catch (e : Exception) {
            logger.error("Unable to send to event hub ${fnConfig.evHubSendName}: ${e.message}")
        }
         return JsonObject()

    } //.eventHubProcessor


    fun getProfileNameAndPaths(hl7Content: String, route: String): Pair<String, List<String>> {
        val routeName = route.uppercase().trim()
        val profileList = fnConfig.profileConfig.profileIdentifiers.filter { it.route.uppercase().trim() == routeName }
        // if the route is not specified in the config file, assume the default of MSH-12
        val profileIdPaths = if (profileList.isNotEmpty()) {
            profileList[0].identifierPaths
        } else listOf(DEFAULT_SPEC_PROFILE)

        val prefix = if (routeName.isNotEmpty()) "$routeName-" else ""
        val profileName = try {
            prefix + profileIdPaths.map { path ->
                HL7StaticParser.getFirstValue(hl7Content, path).get().uppercase()
            }.reduce { acc, map -> "$acc-$map" }
        } catch (e: NoSuchElementException) {
            throw InvalidMessageException("Unable to load validation profile: " +
                    "One or more values in the profile path(s)" +
                    " ${profileIdPaths.joinToString()} are missing.")
        }
        return Pair(profileName, profileIdPaths)

    }

    private fun validateMessage(hl7Message: String, messageType: HL7MessageType, route: String): NistReport {
        validateHL7Delimiters(hl7Message)
        if (messageType == HL7MessageType.ELR && route.isEmpty()) {
            throw InvalidMessageException("ELR message type must have a Route specified.")
        }
        val profileNameAndPaths = getProfileNameAndPaths(hl7Message, route)
        val profileName =  profileNameAndPaths.first
        val profilePaths = profileNameAndPaths.second
        val nistValidator = fnConfig.getNistValidator(profileName)
        if (nistValidator == null) {
            if (messageType == HL7MessageType.ELR) {
                throw InvalidMessageException(
                    "Unable to find validation profile named $profileName."
                            + " Either the route '$route' or the data in HL7 path '${profilePaths.joinToString()}' is invalid."
                )
            } else {
                throw InvalidMessageException("Unable to find validation profile $profileName.")
            }
        } else {
            val report = nistValidator.validate(hl7Message)
            report.status = if ("ERROR" in report.status + "") {
                NIST_INVALID_MESSAGE
            } else if (report.status.isNullOrEmpty()) {
                "Unknown"
            } else {
                report.status + ""
            }

            return report
        }
    }

    private fun validateHL7Delimiters(hl7Message: String) {
        val msg = hl7Message.trim()
        val mshPos = msg.indexOf(HL7_MSH)
        if (mshPos > -1) {
            val delimiters = msg.substring(mshPos + HL7_MSH.length, mshPos + (HL7_MSH.length + HL7_SUBDELIMITERS.length))
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
        val messageType = if (!request.headers["x-tp-message_type"].isNullOrEmpty()) {
            request.headers["x-tp-message_type"].toString()
        } else {
            return buildHttpResponse("BAD REQUEST: Message Type ('CASE' or 'ELR') " +
                    "must be specified in the HTTP Header as 'x-tp-message_type'. " +
                    "Please correct the HTTP header and try again.",
                HttpStatus.BAD_REQUEST,
                request)
        }
        logger.info("Received message with x-tp-message_type $messageType")

        val route = if (!request.headers["x-tp-route"].isNullOrEmpty()) {
            request.headers["x-tp-route"].toString()
        } else {
            if (messageType == "ELR") {
                return buildHttpResponse("BAD REQUEST: ELR message must specify a route" +
                        " in the HTTP header as 'x-tp-route'. " +
                        "Please correct the HTTP header and try again.",
                    HttpStatus.BAD_REQUEST,
                    request)
            } else {
                ""
            }
        }
        if (route.isNotEmpty()) { logger.info("Message route received: $route") }
        return try {
            val report =  validateMessage(
                    hl7Message, messageType.let { HL7MessageType.valueOf(it) }, route
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

    private fun buildHttpResponse(message:String, status: HttpStatus, request: HttpRequestMessage<Optional<String>>) : HttpResponseMessage {
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