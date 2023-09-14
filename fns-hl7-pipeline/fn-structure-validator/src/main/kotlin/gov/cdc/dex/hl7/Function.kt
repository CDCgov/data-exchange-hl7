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
        private const val PHIN_SPEC_PROFILE = "MSH-21[1].1" //Not able to use HL7-PET due to scala version conflicts with NistValidator.
        private const val ELR_SPEC_PROFILE = "MSH-12"
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
        @BindingName("SystemPropertiesArray") eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext,
        @EventHubOutput(name="structureOk",
            eventHubName = "%EventHubSendOkName%",
            connection = "EventHubConnectionString") structureOkOutput : OutputBinding<List<String>>,
        @EventHubOutput(name="structureErr",
            eventHubName = "%EventHubSendErrsName%",
            connection = "EventHubConnectionString") structureErrOutput: OutputBinding<List<String>>,
        @CosmosDBOutput(name="cosmosdevpublic",
            connection = "CosmosDBConnectionString",
            containerName = "hl7-structure", createIfNotExists = true,
            partitionKey = "/message_uuid", databaseName = "hl7-events") cosmosOutput: OutputBinding<List<JsonObject>>
    ): JsonObject {
        logger.info("Function triggered. Version: ${fnConfig.functionVersion}")
        val outOkList = mutableListOf<String>()
        val outErrList = mutableListOf<String>()
        val outEventList = mutableListOf<JsonObject>()
        try {
            message.forEachIndexed { msgNumber: Int, singleMessage: String? ->
                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
                val startTime = Date().toIsoString()
                var report = NistReport()
                var metadata = JsonObject()

                try {
                    val hl7Content = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
                    metadata = JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject
                    val filePath = JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                    val messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString
                    val messageType = JsonHelper.getValueFromJson("message_info.type", inputEvent).asString
                    val routeJson = JsonHelper.getValueFromJson("message_info.route", inputEvent)
                    val route = if (routeJson is JsonPrimitive) routeJson.asString else ""

                    // set id parameter if does not exist
                    if (!inputEvent.has("id")) {
                        inputEvent.add("id", messageUUID.toJsonElement())
                    }

                    logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath, messageType: $messageType")
                    //Main FN Logic
                    report =
                        validateMessage(hl7Content, messageUUID, filePath, HL7MessageType.valueOf(messageType), route)
                    //preparing EventHub payload:
                    val processMD = StructureValidatorProcessMetadata(
                        PROCESS_STATUS_OK,
                        report,
                        eventHubMD[msgNumber],
                        listOf(getProfileName(hl7Content, HL7MessageType.valueOf(messageType), route))
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

                    // add event to appropriate output binding parameter
                    var destIndicator = "OK"
                    if (NIST_VALID_MESSAGE == report.status) {
                        outOkList.add(gson.toJson(inputEvent))
                    } else {
                        destIndicator = "ERROR"
                        outErrList.add(gson.toJson(inputEvent))
                    }
                    outEventList.add(gson.toJsonTree(inputEvent) as JsonObject)
                    logger.info("Processed $destIndicator structure validation for messageUUID: $messageUUID, filePath: $filePath, report.status: ${report.status}")

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
                    val problem = Problem(StructureValidatorProcessMetadata.VALIDATOR_PROCESS, e, false, 0, 0)
                    val summary = SummaryInfo(NIST_INVALID_MESSAGE, problem)

                    logger.error("metadata in exception: $metadata")
                    logger.info("inputEvent in exception:$inputEvent")

                    inputEvent.add("summary", summary.toJsonElement())
                    outErrList.add(gson.toJson(inputEvent))
                    outEventList.add(gson.toJsonTree(inputEvent) as JsonObject)
                    logger.info(
                        "Sent Message to Err Event Hub for Message Id ${
                            JsonHelper.getValueFromJson(
                                "message_uuid",
                                inputEvent
                            ).asString
                        }"
                    )
                }
            } // foreachIndexed
        } catch (ex: Exception) {
            logger.error("An unexpected error occurred: ${ex.message}")
        } finally {
            structureOkOutput.value = outOkList
            structureErrOutput.value = outErrList
            cosmosOutput.value = outEventList
        }
        return if (outEventList.isNotEmpty()) {
            outEventList.last()
        } else {
            JsonObject()
        }
    } //.eventHubProcessor


    private fun getProfileName(hl7Content: String, messageType: HL7MessageType, route: String): String {
        val profileName:String =
            when (messageType) {
                HL7MessageType.CASE -> HL7StaticParser.getFirstValue(hl7Content, PHIN_SPEC_PROFILE).get()
                    .uppercase(Locale.getDefault())
                HL7MessageType.ELR -> "${route.uppercase()}-v${HL7StaticParser.getFirstValue(hl7Content, ELR_SPEC_PROFILE).get().uppercase()}"
                else -> throw InvalidMessageException("Invalid Message Type: $messageType. Please specify CASE or ELR")
            }
        return profileName
    }

    private fun validateMessage(hl7Message: String, messageUUID: String, filePath: String, messageType: HL7MessageType, route: String): NistReport {
        validateHL7Delimiters(hl7Message)
        val profileName =  try {
            getProfileName(hl7Message, messageType, route)
        } catch (e: NoSuchElementException) {
            logger.error("Unable to retrieve Profile Name")
            val exMessage = if (filePath != "N/A") {  " for messageUUID: $messageUUID, filePath: $filePath." }
                else { "." } // do not add file information if processed via https request
            throw InvalidMessageException("Unable to process message: Unable to retrieve PHIN Specification from $PHIN_SPEC_PROFILE$exMessage")
        }

        val nistValidator = fnConfig.nistValidators[profileName]
        if (nistValidator == null) {
            throw InvalidMessageException("Unsupported route $profileName")
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
                    hl7Message, "N/A", "N/A",
                    messageType.let { HL7MessageType.valueOf(it) }, route
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