package gov.cdc.dataexchange

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.azure.functions.OutputBinding
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dataexchange.services.ServiceUtil.Companion.mapMessages
import org.slf4j.LoggerFactory

class Function {

    companion object {
        private var logger = LoggerFactory.getLogger(Function::class.java.simpleName)
        private var objectMapper = ObjectMapper()
    }

    @FunctionName("branch")
    fun branch(
        @EventHubTrigger(
            name = "msg",
            eventHubName = "%EventHubReceiveName%",
            connection = "EventHubConnectionString",
            consumerGroup = "%EventHubConsumerGroup%"
        ) messages: List<String>?,
        @EventHubOutput(name="recdebOk",
            eventHubName = "%EventHubSendOkName%",
            connection = "EventHubConnectionString")
        branchOkOutput : OutputBinding<List<String>>,
        @EventHubOutput(name="recdebErr",
            eventHubName = "%EventHubSendErrsName%",
            connection = "EventHubConnectionString")
        branchErrOutput: OutputBinding<List<String>>
    ) {
        logger.info("DEX::Received event!")

        if (messages.isNullOrEmpty()) {
            logger.error("DEX::Unable to map messages. No messages found.")
            return
        }

        logger.info("DEX::Mapping ${messages.size} messages.")
        val mappedMessages: List<Map<String, Any>> = mapMessages(messages)

        val outOkList = mutableListOf<String>()
        val outErrList = mutableListOf<String>()

        try {
            mappedMessages.forEachIndexed { i: Int, mappedMessage: Map<String, Any> ->
                try {
                    val messageUuid = mappedMessage["message_uuid"] as String

                    val lastProcess: Map<String, Any> = ((mappedMessage["metadata"] as Map<String, Any>)["processes"] as List<Map<String, Any>>).last()
                    val processName: String = lastProcess["process_name"] as String
                    val processStatus: String = lastProcess["status"] as String

                    if (processStatus == "SUCCESS") {
                        logger.info("DEX::To OK eventhub [${i + 1}] message_uuid: $messageUuid, processName=$processName, status=$processStatus")
                        outOkList.add(objectMapper.writeValueAsString(mappedMessage))
                    } else {
                        logger.info("DEX::To ERR eventhub [${i + 1}] message_uuid: $messageUuid, processName=$processName, status=$processStatus")
                        outErrList.add(objectMapper.writeValueAsString(mappedMessage))
                    }
                } catch (e: Exception) {
                    // TODO send to quarantine?
                    logger.error("Error processing message", e)
                }
            }
        } finally {
            branchOkOutput.value = outOkList
            branchErrOutput.value = outErrList
        }
    }
}
