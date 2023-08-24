package gov.cdc.dex.hl7

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.OutputBinding
import com.microsoft.azure.functions.annotation.EventHubOutput
import com.microsoft.azure.functions.annotation.EventHubTrigger
import com.microsoft.azure.functions.annotation.FunctionName
import gov.cdc.dex.util.JsonHelper
import org.slf4j.LoggerFactory

/**
 * Azure Functions with Event Hub trigger.
 */
class EventHubTriggerOut {
    /**
     * This function will be invoked when an event is received from Event Hub.
     */
    companion object {
        val fnConfig = FunctionConfig()
        val gson: Gson = GsonBuilder().serializeNulls().create()
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
    }
    data class OutputMessages(val messagesOk : MutableList<String>, val messagesErr : MutableList<String>)

    @FunctionName("EventHubTriggerOut-recdeb")
    fun run(
        @EventHubTrigger(
            name = "message",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%"
        )
        messages: List<String?>,
        @EventHubOutput(
            name ="outputEventhubok",
            eventHubName =  "%EventHubSendOkName%",
            connection = "EventHubConnectionString")
        outputMessagesOk: OutputBinding<List<String>>,
        @EventHubOutput(
            name ="outputEventhuberr",
            eventHubName =  "%EventHubSendErrsName%",
            connection = "EventHubConnectionString")
        outputMessagesErr: OutputBinding<List<String>>,

        )
     {
         var messagesOk = mutableListOf<String>()
         var messagesErr = mutableListOf<String>()
         messages.forEachIndexed {
                 messageIndex: Int, singleMessage: String? ->
              try {
                 val (messagesOk, messagesErr) =  sendMessageToEventHub(singleMessage)
                  outputMessagesOk.value = messagesOk
                  outputMessagesErr.value = messagesErr

                  logger.info("DEX:: messagesOk ${messagesOk.size}")
                  logger.info("DEX:: messagesErr ${messagesErr.size}")

             } catch(e:Exception){
                  logger.error("DEX:: Unable to process Message due to exception: ${e.message}")
             }
         }

    }



    private fun sendMessageToEventHub(message : String?) : OutputMessages {
        var messagesOk = mutableListOf<String>()
        var messagesErr = mutableListOf<String>()
        try {
            val inputEvent: JsonObject = JsonParser.parseString(message) as JsonObject
            val messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString
            val metadata = inputEvent["metadata"].asJsonObject
            val provenance = metadata["provenance"].asJsonObject
            val filePath = provenance["file_path"].asString
            val processesArr = metadata["processes"].asJsonArray
            val lastProcess = processesArr.last()
            val status = lastProcess.asJsonObject["status"].asString
            val eventHubReceiverName =getEventhubName(lastProcess.asJsonObject["status"].asString)
            logger.info("DEX::Processed and Sent to event hub $eventHubReceiverName Message: --> messageUUID: ${messageUUID} -->filepath:$filePath")
            if (status.equals("SUCCESS"))
                messagesOk.add(gson.toJson(inputEvent))
            else
                messagesErr.add(gson.toJson(inputEvent))
           // fnConfig.evHubSender.send(eventHubReceiverName, gson.toJson(inputEvent))
        } catch(e: Exception){
            logger.error("DEX:: Unable to process Message due to exception: ${e.message}")
        }
        return OutputMessages (messagesOk,messagesErr)
    }

    private fun getEventhubName(status : String): String {
        return when (status){
            "SUCCESS" -> fnConfig.evHubOkName
            else -> fnConfig.evHubErrorName
        }

    }
}