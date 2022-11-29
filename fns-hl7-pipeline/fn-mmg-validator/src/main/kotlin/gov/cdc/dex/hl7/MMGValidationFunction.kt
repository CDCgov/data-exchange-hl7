package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.hl7.model.MmgReport
import gov.cdc.dex.hl7.model.MmgValidatorProcessMetadata
import gov.cdc.dex.hl7.model.ValidationReport
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper.addArrayElement
import java.util.*

/**
 * Azure Functions with Event Hub Trigger.
 */
class MMGValidationFunction: AzFnMsgProcessor() {

    private val gson = Gson()

    @FunctionName("mmgvalidator001")
    fun eventHubProcessor(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%",
        )
        messages: List<String?>,
        context: ExecutionContext
    ) {
       exec(messages, context)
    }

    override fun getProcessName(): String {
        return MmgValidatorProcessMetadata.MMG_VALIDATOR_PROCESS
    }

    override fun prepareSummary(status: String, inputEvent: JsonObject) {
        val summary = SummaryInfo(status)
        if (ValidationReport.VALID_MESSAGE != status) {
            summary.problem = Problem(
                getProcessName(), null, null,
                "Message failed MMG Validation",
                false,0,0)
        }
        inputEvent.add("summary", JsonParser.parseString(gson.toJson(summary)))
    }

    override fun addProcessStatus(startTime: String, metadata: JsonObject, status: String, report: ValidationReport) {
        val processMD = MmgValidatorProcessMetadata(report.status, report)
        processMD.startProcessTime = startTime
        processMD.endProcessTime = Date().toIsoString()

        metadata.addArrayElement("processes", processMD)
    }

    override fun validateMessage(hl7Content: String): ValidationReport {
        val mmgValidator = MmgValidator()
        val validationReport = mmgValidator.validate(hl7Content)
        return MmgReport(validationReport)
    }
}

