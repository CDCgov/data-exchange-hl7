package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.azure.EventHubMetadata
import gov.cdc.dex.azure.EventHubSender
import gov.cdc.dex.hl7.model.Segment
import gov.cdc.dex.metadata.Problem
import gov.cdc.dex.metadata.SummaryInfo
import gov.cdc.dex.util.DateHelper.toIsoString
import gov.cdc.dex.util.JsonHelper
import gov.cdc.dex.util.JsonHelper.addArrayElement
import gov.cdc.dex.util.JsonHelper.toJsonElement
import java.util.*
import org.slf4j.LoggerFactory

@FunctionName("LAKE_OF_SEGMENTS_TRANSFORMER_ELR")
fun eventHubELRProcessor(
    @EventHubTrigger(
        name = "msg",
        eventHubName = "%EventHubReceiveNameELR%",
        connection = "EventHubConnectionString",
        consumerGroup = "%EventHubConsumerGroupELR%",)
    message: List<String?>,
    @BindingName("SystemPropertiesArray")eventHubMD:List<EventHubMetadata>,
    context: ExecutionContext) : JsonObject {

    return processMessages(message, eventHubMD)

} // .eventHubProcessor