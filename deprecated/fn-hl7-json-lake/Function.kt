package gov.cdc.dex.hl7

import com.google.gson.*
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.hl7.bumblebee.HL7JsonTransformer
import java.util.*
import com.google.gson.JsonObject


/**
 * Azure function with event hub trigger for the HL7 JSON Lake transformer
 * Takes an HL7 message and converts it to an HL7 json lake based on the PhinGuidProfile
 */

@FunctionName("HL7_JSON_LAKE_TRANSFORMER_ELR")
fun eventHubELRProcessor(
    @EventHubTrigger(
        name = "msg",
        eventHubName = "%EventHubReceiveNameELR%",
        connection = "EventHubConnectionString",
        consumerGroup = "%EventHubConsumerGroupELR%",)
    messages: List<String?>,
    @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
    context: ExecutionContext): JsonObject {
    //
    // Process each Event Hub Message
    // ----------------------------------------------
    // message.forEach { singleMessage: String? ->
    return processAllMessages(messages, eventHubMD, context)

} // .eventHubProcessor