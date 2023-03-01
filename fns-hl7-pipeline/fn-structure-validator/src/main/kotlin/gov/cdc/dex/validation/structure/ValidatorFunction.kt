package gov.cdc.dex.validation.structure

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import gov.cdc.dex.azure.EventHubSender

import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.model.StructureValidatorProcessMetadata
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.util.UnknownPropertyError
import gov.cdc.hl7.HL7StaticParser
import gov.cdc.nist.validator.InvalidFileException

import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.slf4j.LoggerFactory
import java.util.*
import java.util.logging.Logger

private lateinit var evHubNameOk: String
private lateinit var evHubNameErrs: String
private lateinit var evHubConnStr: String
private lateinit var log: Logger

class ValidatorFunction {
    companion object {
        private const val PHIN_SPEC_PROFILE = "MSH-21[1].1" //Not able to use HL7-PET due to scala version conflicts with NistValidator.
        private const val NIST_VALID_MESSAGE = "VALID_MESSAGE"

        private val logger = LoggerFactory.getLogger(ValidatorFunction::class.java.simpleName)
        val gson = GsonBuilder().serializeNulls().create()
    }
    /**
     * This function will be invoked when an event is received from Event Hub.
     */
    @FunctionName("NIST-VALIDATOR")
    fun run(
        @EventHubTrigger(
            name = "message",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%",
            // cardinality = Cardinality.MANY
        ) message: List<String?>,
        context: ExecutionContext
    ) {
       // context.logger.info("Event received message.size: ${message.size}")
        log = context.logger
        log.info("Event Message received: $message.")

        setConnectionStrings()
        val ehSender = EventHubSender(evHubConnStr)

        message.forEach { singleMessage: String? ->
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            // context.logger.info("singleMessage: --> $singleMessage")
            val hl7Content : String
            val metadata : JsonObject
            val filePath : String
            val messageUUID : String
            try {

                hl7Content = getEncodedValueFromJson("content", inputEvent)
                metadata = getObjectFromJson("metadata", inputEvent)
                filePath = getObjectFromJson("metadata.provenance.file_path", inputEvent).asString
                messageUUID = getObjectFromJson("message_uuid", inputEvent).asString
                log.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                var phinSpec = getPhinSpec(hl7Content, messageUUID, filePath)

                nistValidator(
                    phinSpec,
                    hl7Content,
                    metadata,
                    inputEvent,
                    messageUUID,
                    filePath,
                    ehSender
                )

            } catch (e: Exception) {
                //TODO::  - update retry counts
                log.severe("Unable to process Message due to exception: ${e.message}")
                val problem = Problem(StructureValidatorProcessMetadata.VALIDATOR_PROCESS, e, false, 0, 0)
                val summary = SummaryInfo("STRUCTURE_ERROR", problem)
                inputEvent.add("summary", summary.toJsonElement())
                ehSender.send(evHubNameErrs, Gson().toJson(inputEvent))
            }
        }
    }

    private fun nistValidator(
        phinSpec: String?,
        hl7Content: String,
        metadata: JsonObject,
        inputEvent: JsonObject,
        messageUUID: String,
        filePath: String,
        ehSender: EventHubSender
    ) {
        try {
            val startTime =  Date().toIsoString()

            log.fine("Processing Structure Validation for profile $phinSpec")
            val nistValidator = ProfileManager(ResourceFileFetcher(), "/${phinSpec?.uppercase()}")
            val report = nistValidator.validate(hl7Content)
            val processMD = StructureValidatorProcessMetadata(report.status ?: "Unknown", report)
            processMD.startProcessTime = startTime
            processMD.endProcessTime = Date().toIsoString()

            metadata.addArrayElement("processes", processMD)
            //Update Summary element.
            val summary = SummaryInfo(report.status ?: "Unknown")
            if (NIST_VALID_MESSAGE != report.status) {
                summary.problem =
                    Problem(StructureValidatorProcessMetadata.VALIDATOR_PROCESS, "Message failed Structure Validation")
            }
            inputEvent.add("summary", JsonParser.parseString(gson.toJson(summary)))
            //Send event
            val ehDestination = if (NIST_VALID_MESSAGE == report.status) evHubNameOk else evHubNameErrs


            log.info("Processed structure validation for messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination, report.status: ${report.status}")
            log.finest("INPUT EVENT OUT: --> ${gson.toJson(inputEvent)}")
            ehSender.send(ehDestination, Gson().toJson(inputEvent))
        } catch (e: InvalidFileException) {
            throw InvalidMessageException("Unable to process Message due to PHIN Spec: $phinSpec not recognized.  messageUUID: $messageUUID, filePath: $filePath")
        }
    }

    private fun getEncodedValueFromJson(
        property: String,
        inputEvent: JsonObject
    ): String {
        var value: String?
        try {
            value = JsonHelper.getValueFromJsonAndBase64Decode(property, inputEvent)
        } catch (e: UnknownPropertyError) {
            throw InvalidMessageException("Unable to process Message: Unparsable JSON content.")
        }
        return value
    }

    private fun getObjectFromJson(
        property: String,
        inputEvent: JsonObject
    ): JsonObject {
        var jsonObject: JsonObject?
        try {

            jsonObject = JsonHelper.getValueFromJson(property, inputEvent).asJsonObject
        } catch (e: UnknownPropertyError) {
            throw InvalidMessageException("Unable to process Message: Unparsable JSON content.")
        }
        return jsonObject
    }
    private fun getPhinSpec(
        hl7Content: String,
        messageUUID: String,
        filePath: String
    ): String? {
        var phinSpec: String?
        try {
            phinSpec = HL7StaticParser.getFirstValue(hl7Content, PHIN_SPEC_PROFILE).get()
        } catch (e: NoSuchElementException) {
            throw InvalidMessageException("Unable to process Message: Unable to retrieve PHIN Specification from message, MSH-21[1].1 Not found - messageUUID: $messageUUID, filePath: $filePath. ")
        }
        return phinSpec
    }

    /**
     * Gather environment variables; Throw an exception if any are missing.
     */
    private fun setConnectionStrings() {

        getEnvVar("EventHubSendOkName").also { connectionString ->
            if (connectionString.isNullOrEmpty()) {
                throw Exception("EventHubSendOkName Not Set")
            }
            evHubNameOk = connectionString
        }

        getEnvVar("EventHubSendErrsName").also { connectionString ->
            if (connectionString.isNullOrEmpty()) {
                throw Exception("Dead Letter Topic Token Not Set")
            }
            evHubNameErrs = connectionString
        }

        getEnvVar("EventHubConnectionString").also { connectionString ->
            if (connectionString.isNullOrEmpty()) {
                throw Exception("Valid Topic Token Not Set")
            }
            evHubConnStr = connectionString
        }
    }
    fun getEnvVar(varName: String): String? = System.getenv(varName)

    @FunctionName("structure")
    fun invoke(
        @HttpTrigger(name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS)
        request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext): HttpResponseMessage {

        val hl7Message : String?
        try {
            hl7Message = request.body?.get().toString()
        } catch (e: NoSuchElementException) {
            return buildHttpResponse(
                "No body was found. Please send an HL7 v.2.x message in the body of the request.",
                HttpStatus.BAD_REQUEST,
                request
            )
        }

        val phinSpec: String?
        val nistValidator: ProfileManager?
        try {
            phinSpec = HL7StaticParser.getFirstValue(hl7Message, PHIN_SPEC_PROFILE).get()
        } catch (e: NoSuchElementException) {
            return buildHttpResponse(
                "The profile (MSH-21.1) is missing.",
                HttpStatus.EXPECTATION_FAILED,
                request
            )
        }

        context.logger.info("Validating message with SPECCCCCC: $phinSpec")
        nistValidator = ProfileManager(ResourceFileFetcher(), "/${phinSpec?.uppercase()}")

        return runCatching {
            val report = nistValidator.validate(hl7Message)
            buildHttpResponse(gson.toJson(report), HttpStatus.OK, request)
        }.onFailure { exception ->
            buildHttpResponse(
                "Please pass an HL7 message on the request body.",
                HttpStatus.BAD_REQUEST,
                request
            )
        }.getOrThrow()
    }

}

private fun buildHttpResponse(message:String, status: HttpStatus, request: HttpRequestMessage<Optional<String>>) : HttpResponseMessage {
    var contentType : String = "application/json"
    if (status != HttpStatus.OK) {
        contentType = "text/plain"
    }
    return request
        .createResponseBuilder(status)
        .header("Content-Type", contentType)
        .body(message)
        .build()
}