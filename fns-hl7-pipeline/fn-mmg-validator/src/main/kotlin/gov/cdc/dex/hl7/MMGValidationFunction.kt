package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.hl7.model.MmgReport
import gov.cdc.dex.hl7.model.MmgValidatorProcessMetadata
import gov.cdc.dex.hl7.model.ReportStatus
import gov.cdc.dex.hl7.model.ValidationIssue
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.mmg.InvalidConditionException
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import java.util.*

/**
 * Azure Functions with Event Hub Trigger.
 */
class MMGValidationFunction {
    
    companion object {
        private const val STATUS_ERROR = "ERROR"
        
        val gson = Gson()
    } // .companion

    @FunctionName("mmgvalidator001")
    fun eventHubProcessor(
        @EventHubTrigger(
                name = "msg", 
                eventHubName = "%EventHubReceiveName%",
                connection = "EventHubConnectionString",
                consumerGroup = "%EventHubConsumerGroup%",) 
                message: List<String?>,
        @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
        context: ExecutionContext) {

        val startTime =  Date().toIsoString()
        // context.logger.info("received event: --> $message")
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val eventHubSendOkName = System.getenv("EventHubSendOkName")
        val eventHubSendErrsName = System.getenv("EventHubSendErrsName")
        val evHubSender = EventHubSender(evHubConnStr)

        message.forEachIndexed {
                messageIndex : Int, singleMessage: String? ->
            val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject
            // context.logger.info("singleMessage: --> $singleMessage")
            try {
                val hl7ContentBase64 = JsonHelper.getValueFromJson("content", inputEvent).asString
                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = String(hl7ContentDecodedBytes)
                val metadata = inputEvent["metadata"].asJsonObject
                val filePath = JsonHelper.getValueFromJson("metadata.provenance.file_path", inputEvent).asString
                val messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString

                try {
                    context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")
                    val mmgValidator = MmgValidator()
                    val validationReport = mmgValidator.validate(hl7Content)
                    context.logger.info("MMG Validation Report size for for messageUUID: $messageUUID, filePath: $filePath, size --> " + validationReport.size)
                    val mmgReport = MmgReport( validationReport)

                    // document the MMGs used to validate the message
                    var mmgInfo = try {
                        JsonHelper.getStringArrayFromJsonArray(JsonHelper.getValueFromJson("message_info.mmgs", inputEvent).asJsonArray)
                    } catch (e: Exception) {
                        arrayOf()
                    }
                    // 'validate' function is actually getting its own list of MMGs based on the message --
                    // so, if the message_info list is empty/null, we can try to get it from mmgValidator
                    // (exception will already have been thrown if mmgValidator could not determine the MMGs)
                    if (mmgInfo.isEmpty() && mmgValidator.mmgs.isNotEmpty()) {
                        mmgInfo = mmgValidator.mmgs.map { "mmg:${it.name}" }.toTypedArray()
                    }
                    val processMD = MmgValidatorProcessMetadata(mmgReport.status.toString(), mmgReport,eventHubMD[messageIndex],
                        mmgInfo.toList())
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()

                    metadata.addArrayElement("processes", processMD)
                    //Prepare Summary:
                    val summary = SummaryInfo(mmgReport.status.toString())
                    if (ReportStatus.MMG_ERRORS == mmgReport.status ) {
                        summary.problem= Problem(MmgValidatorProcessMetadata.MMG_VALIDATOR_PROCESS, null, null, "Message failed MMG Validation", false, 0, 0)
                    }
                    inputEvent.add("summary", JsonParser.parseString(gson.toJson(summary)))
                    //Send event
                    context.logger.info("INPUT EVENT OUT: --> ${ gson.toJson(inputEvent) }")

                    val ehDestination = if (mmgReport.status == ReportStatus.MMG_VALID) eventHubSendOkName else eventHubSendErrsName
                    evHubSender.send(evHubTopicName=ehDestination, message=gson.toJson(inputEvent))
                    context.logger.info("Processed for MMG validated messageUUID: $messageUUID, filePath: $filePath, ehDestination: $ehDestination, reportStatus: $mmgReport")


                } catch (e: Exception) {
                    //TODO::  - update retry counts
                    context.logger.severe("Unable to process Message due to exception: ${e.message}")
                    val processMD = MmgValidatorProcessMetadata ("MMG_VALIDATOR_EXCEPTION", null, eventHubMD[messageIndex], listOf())
                    processMD.startProcessTime = startTime
                    processMD.endProcessTime = Date().toIsoString()
                    metadata.addArrayElement("processes", processMD)

                    val problem = Problem(MmgValidatorProcessMetadata.MMG_VALIDATOR_PROCESS, e, false, 0, 0)
                    val summary = SummaryInfo(STATUS_ERROR, problem)
                    inputEvent.add("summary", summary.toJsonElement())

                    evHubSender.send(evHubTopicName = eventHubSendErrsName, message = Gson().toJson(inputEvent))
                    // e.printStackTrace()
                }
            } catch (e: Exception) {
                context.logger.severe("Exception processing event hub message: Unable to process Message due to exception: ${e.message}")
                evHubSender.send(evHubTopicName = eventHubSendErrsName, message = Gson().toJson(inputEvent))
                e.printStackTrace()
            }
        } // .message.forEach
    } // .eventHubProcessor

    @FunctionName("validate-mmg")
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

        val mmgValidator : MmgValidator
        val validationReport : List<ValidationIssue>
        try {
            context.logger.info("Validating message...")
            mmgValidator = MmgValidator()
            validationReport = mmgValidator.validate(hl7Message)
        } catch (e : Exception) {
            if (e is NoSuchElementException || e is InvalidConditionException) {
                return buildHttpResponse("Error: ${e.message.toString()}",
                    HttpStatus.BAD_REQUEST,
                    request)
            }
            return buildHttpResponse("An unexpected error occurred: ${e.message.toString()}",
                HttpStatus.BAD_REQUEST,
                request)
        }
        val report = MmgReport(validationReport)
        return buildHttpResponse(gson.toJson(report), HttpStatus.OK, request)
    }
    private fun buildHttpResponse(message:String, status: HttpStatus, request: HttpRequestMessage<Optional<String>>) : HttpResponseMessage {
        var contentType = "application/json"
        if (status != HttpStatus.OK) {
            contentType = "text/plain"
        }
        return request
            .createResponseBuilder(status)
            .header("Content-Type", contentType)
            .body(message)
            .build()
    }

} // .Function

