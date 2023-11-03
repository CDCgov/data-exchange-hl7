package gov.cdc.dataexchange

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.microsoft.azure.functions.annotation.*
import org.slf4j.LoggerFactory
import gov.cdc.dex.util.JsonHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

class Function {

    companion object {
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        val fnConfig = FunctionConfig()
    }

    @FunctionName("storage-sink")
    fun storageSink(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%"
        ) messages: List<String>?
    ) {
        logger.info("DEX::Received event!")

        if (messages.isNullOrEmpty()) {
            logger.error("DEX::Unable to sink messages. No messages found.")
            return
        }
        val today = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now())
        messages.forEach { message: String ->
            try {
                // extract metadata
                val inputEvent: JsonObject = JsonParser.parseString(message) as JsonObject
                val messageUUID = JsonHelper.getValueFromJson("message_uuid", inputEvent).asString

                // save to storage container
                fnConfig.azureBlobProxy.saveBlobToContainer("$today/$messageUUID.txt", message)

            } catch (e: Exception) {
                // TODO send to quarantine?
                logger.error("Error processing message", e)
            }
        }

    }
}