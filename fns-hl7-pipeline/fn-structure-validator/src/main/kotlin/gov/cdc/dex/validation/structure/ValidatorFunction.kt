package gov.cdc.dex.validation.structure

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.metadata.HL7MessageType
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.model.StructureValidatorProcessMetadata
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.HL7StaticParser
import gov.cdc.nist.validator.NistReport
import gov.cdc.nist.validator.ProfileManager
import gov.cdc.nist.validator.ResourceFileFetcher
import org.slf4j.LoggerFactory
import java.util.*


class ValidatorFunction {
    companion object {
        private const val PHIN_SPEC_PROFILE = "MSH-21[1].1" //Not able to use HL7-PET due to scala version conflicts with NistValidator.
        private const val ELR_SPEC_PROFILE = "MSH-12"
        private const val NIST_VALID_MESSAGE = "VALID_MESSAGE"

        private var logger = LoggerFactory.getLogger(ValidatorFunction::class.java.simpleName)
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
            cardinality = Cardinality.MANY
        ) message: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext
    ) {

        val startTime =  Date().toIsoString()

        // context.logger.info("Event received message.size: ${message.size}")
        val log = context.logger
        log.info("Event Message received: $message.")

        val evHubNameOk = getSafeEnvVariable("EventHubSendOkName")
        val evHubNameErrs = getSafeEnvVariable("EventHubSendErrsName")
        val evHubConnStr = getSafeEnvVariable("EventHubConnectionString")
        var evHubNameELROk = getSafeEnvVariable("EventHubSendELROkName")

        val ehSender = EventHubSender(evHubConnStr)

        var nbrOfMessages = 0
        message.forEach { singleMessage: String? ->
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            // context.logger.info("singleMessage: --> $singleMessage")
            var report : NistReport = NistReport()
            var metadata :JsonObject = JsonObject()

            try {
                val hl7Content = JsonHelper.getValueFromJsonAndBase64Decode("content", inputEvent)
                metadata = JsonHelper.getValueFromJson("metadata", inputEvent).asJsonObject
                val filePath =JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                val messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString

                val messageType = JsonHelper.getValueFromJson("message_type", inputEvent).asString
                val route = JsonHelper.getValueFromJson("message_info.route", inputEvent).asString

                log.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath, messageType: $messageType")
                //Main FN Logic
                //val phinSpec = getProfile(hl7Content, messageUUID, filePath, PHIN_SPEC_PROFILE)
                val report = validateMessage(hl7Content, messageUUID, filePath, HL7MessageType.valueOf(messageType), route)
                //preparing EventHub payload:
                val processMD = StructureValidatorProcessMetadata(report.status ?: "Unknown", report, eventHubMD[nbrOfMessages], listOf(getProfileName(hl7Content, HL7MessageType.valueOf(messageType), route )))

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
                val ehDestination = getEhDestination(messageType, report, evHubNameOk, evHubNameELROk, evHubNameErrs)
                //val ehDestination = if (NIST_VALID_MESSAGE == report.status) evHubNameOk else evHubNameErrs

                log.info("Processed structure validation for messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination, report.status: ${report.status}")
                log.finest("INPUT EVENT OUT: --> ${gson.toJson(inputEvent)}")
                ehSender.send(ehDestination, Gson().toJson(inputEvent))

            } catch (e: Exception) {
                //TODO::  - update retry counts
                log.severe("Unable to process Message due to exception: ${e.message}")
                val processMD = StructureValidatorProcessMetadata(report.status ?: "Unknown", report, eventHubMD[nbrOfMessages], listOf())
                processMD.startProcessTime = startTime
                processMD.endProcessTime = Date().toIsoString()
                metadata.addArrayElement("processes", processMD)
                val problem = Problem(StructureValidatorProcessMetadata.VALIDATOR_PROCESS, e, false, 0, 0)
                val summary = SummaryInfo("STRUCTURE_ERROR", problem)
                log.severe("metadata in exception: $metadata")
                log.info("inputEvent in exception:$inputEvent")
                inputEvent.add("summary", summary.toJsonElement())
                ehSender.send(evHubNameErrs, Gson().toJson(inputEvent))
            }
            nbrOfMessages++
        }
    }

    private fun getEhDestination(
        messageType: String,
        report: NistReport,
        evHubNameOk: String,
        evHubNameELROk: String,
        evHubNameErrs: String
    ): String {
        return if (messageType == "ELR" && NIST_VALID_MESSAGE == report.status) {
            evHubNameELROk
        } else if (messageType == "CASE" && NIST_VALID_MESSAGE == report.status) {
            evHubNameOk
        } else {
            evHubNameErrs
        }
    }

    private fun getProfileName(hl7Content: String, messageType: HL7MessageType, route: String): String {
        val profileName:String =
            when (messageType) {
                HL7MessageType.CASE -> HL7StaticParser.getFirstValue(hl7Content, PHIN_SPEC_PROFILE).get()
                    .uppercase(Locale.getDefault())

                HL7MessageType.ELR -> "$route-v${HL7StaticParser.getFirstValue(hl7Content, ELR_SPEC_PROFILE).get()}"
                else -> throw InvalidMessageException("Invalid Message Type: $messageType. Please specify CASE or ELR")
            }
        return profileName
    }



    private fun getSafeEnvVariable(varName: String): String {
        val varValue = System.getenv(varName)
        if (varValue.isNullOrEmpty()) {
            throw Exception("$varName Not Set")
        }
        return varValue
    }


    private fun validateMessage(hl7Message: String, messageUUID: String, filePath: String, messageType: HL7MessageType, route: String): NistReport {
       var profileName = ""
        try {
            profileName = getProfileName(hl7Message, messageType, route)
            val nistValidator = ProfileManager(ResourceFileFetcher(), "/$profileName")
            return nistValidator.validate(hl7Message)

        } catch (e: NoSuchElementException) {
            logger.error("Unable to retrieve Profile for $profileName")
            throw InvalidMessageException("Unable to process Message: Unable to retrieve PHIN Specification from message for $profileName,  - messageUUID: $messageUUID, filePath: $filePath. ")
        }
    }

    @FunctionName("structure")
    fun invoke(
        @HttpTrigger(name = "req",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS)
        request: HttpRequestMessage<Optional<String>>,
        context: ExecutionContext): HttpResponseMessage {

        val hl7Message = try {
            request.body?.get().toString()
        } catch (e: NoSuchElementException) {
            return buildHttpResponse(
                "No body was found. Please send an HL7 v.2.x message in the body of the request.",
                HttpStatus.BAD_REQUEST,
                request
            )
        }
        //val messageType =  request.queryParameters?.get("message_type").toString()
        return runCatching {
            val messageType = request.headers["x-tp-message_type"]
            val route = request.headers["x-tp-route"]
            val report = route?.let { rt ->
                messageType?.let { HL7MessageType.valueOf(it) }?.let { msgType ->
                    validateMessage(hl7Message, "N/A", "N/A",
                        msgType, rt
                    )
                }
            }
            buildHttpResponse(gson.toJson(report), HttpStatus.OK, request)
        }.onFailure { exception ->
            context.logger.severe("error validating message: ${exception.message}")
            return buildHttpResponse(
                "${exception.message}",
                HttpStatus.BAD_REQUEST,
                request
            )
        }.getOrThrow()

    }

}

private fun buildHttpResponse(message:String, status: HttpStatus, request: HttpRequestMessage<Optional<String>>) : HttpResponseMessage {
    return request
        .createResponseBuilder(status)
        .header("Content-Type", "application/json")
        .body(message)
        .build()
}