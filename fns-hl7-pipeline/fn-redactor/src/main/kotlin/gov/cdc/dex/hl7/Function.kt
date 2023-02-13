package gov.cdc.dex.hl7

import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.*
import gov.cdc.dex.hl7.model.RedactorProcessMetadata
import gov.cdc.dex.hl7.model.RedactorReport
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.hl7.DeIdentifier

import java.util.*



/**
 * Azure function with event hub trigger to redact messages   */
class Function {
    

    @FunctionName("Redactor")
    fun eventHubProcessor(
            @EventHubTrigger(
                name = "msg",
                eventHubName = "%EventHubReceiveName%",
                connection = "EventHubConnectionString",
                consumerGroup = "%EventHubConsumerGroup%",)
                message: List<String?>,
                context: ExecutionContext) {
//    fun  runVocab(
//        @HttpTrigger(
//            name = "req",
//            methods = [HttpMethod.GET, HttpMethod.POST],
//            authLevel = AuthorizationLevel.ANONYMOUS)
//        request: HttpRequestMessage<Optional<String>>,
//        context : ExecutionContext): HttpResponseMessage {


         context.logger.info("------ received event: ------> message: --> $message")
        val REDACTOR_STATUS_OK = "PROCESS_REDACTOR_OK"
        val startTime =  Date().toIsoString()

        message.forEach { singleMessage: String? ->
             context.logger.info("------ singleMessage: ------>: --> $singleMessage")
            try {

                val inputEvent: JsonObject = JsonParser.parseString(singleMessage) as JsonObject

                // Extract from event
                val hl7ContentBase64 = inputEvent["content"].asString
                val hl7ContentDecodedBytes = Base64.getDecoder().decode(hl7ContentBase64)
                val hl7Content = String(hl7ContentDecodedBytes)
                val metadata = inputEvent["metadata"].asJsonObject
                val provenance = metadata["provenance"].asJsonObject
                val filePath = provenance["file_path"].asString
                val messageUUID = inputEvent["message_uuid"].asString

                context.logger.info("Received and Processing messageUUID: $messageUUID, filePath: $filePath")

                var d = DeIdentifier()
                //test msg
                val msg = "PID|1||TickborneTBRD_TC03^^^SendAppName&2.16.840.1.114222.1111&ISO||~^SMITH^HENRY^H.||20000405|M||2054-5^Black or African American^CDCREC|111 FOURTH ST.^APT2^CHICAGO^47^37011^^^^47003|||||||||||2135-2^Hispanic or Latino^CDCREC"

                var rules = this::class.java.getResource("/case_config.txt").readText().lines()

                val report = d.deIdentifyMessage(hl7Content, rules.toTypedArray())

                println("report msg: ${report._1}")
                println("report: ${report._2}")
                val warnings = RedactorReport(report._2().toList())
                println("warning:$warnings")
                val processMD = RedactorProcessMetadata(status =REDACTOR_STATUS_OK, report =warnings)
                processMD.startProcessTime = startTime
                processMD.endProcessTime = Date().toIsoString()
                println("processMD: $processMD")
                metadata.addArrayElement("processes", processMD)
                println("metadata: $metadata")
                inputEvent.asJsonObject.remove("content")
                inputEvent.asJsonObject.add("content",JsonParser.parseString(report._1))
                println("inputEvent:${inputEvent.get("content")}")


            } catch (e: Exception) {

            }


        } // .eventHubProcessor
       // return request.createResponseBuilder(HttpStatus.OK).body("Hello, " ).build();
    }



} // .Function
