package gov.cdc.dataexchange

import com.microsoft.azure.functions.annotation.*
import com.google.gson.JsonNull
import com.google.gson.JsonParser
import gov.cdc.dex.util.JsonHelper
import org.slf4j.LoggerFactory

class Function {

    companion object {
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        private var fnConfig = FunctionConfig()
    }

    @FunctionName("branch")
    fun branch(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%"
        ) messages: List<String?>
    ) {
        logger.info("DEX::Received event!")

        if (messages.isEmpty()) {
            logger.error("DEX::Unable to map messages. No messages found.")
            return
        }

        val outOkList = mutableListOf<String>()
        val outErrList = mutableListOf<String>()

        messages.forEachIndexed { i: Int, message: String? ->
            if (!message.isNullOrEmpty()) {
                try {
                    val mappedMessage = JsonParser.parseString(message).asJsonObject
                    val processes = JsonHelper.getValueFromJson("metadata.processes", mappedMessage).asJsonArray
                    val lastProcess = processes.last().asJsonObject
                    val processName = JsonHelper.getValueFromJson("process_name", lastProcess).asString
                    val messageUuid = mappedMessage["message_uuid"].asString
                    val summaryInfo = mappedMessage["summary"].asJsonObject
                    val currentStatus = summaryInfo["current_status"].asString

                    if (summaryInfo["problem"] is JsonNull) {
                        logger.info("DEX::To OK eventhub [${i + 1}] message_uuid: $messageUuid, processName=$processName, status=$currentStatus")
                        outOkList.add(message)
                    } else {
                        logger.info("DEX::To ERR eventhub [${i + 1}] message_uuid: $messageUuid, processName=$processName, status=$currentStatus")
                        outErrList.add(message)
                    }
                } catch (e: Exception) {
                    // TODO send to quarantine?
                    logger.error("Error processing message", e)
                }
            }
        }
        try {
            if (outOkList.isNotEmpty()) {
                fnConfig.evHubSenderOk.send(outOkList)
            }
            if (outErrList.isNotEmpty()) {
                fnConfig.evHubSenderErr.send(outErrList)
            }
        } catch (e : Exception) {
            logger.error("Error sending to event hubs", e)
        }

    }
}
