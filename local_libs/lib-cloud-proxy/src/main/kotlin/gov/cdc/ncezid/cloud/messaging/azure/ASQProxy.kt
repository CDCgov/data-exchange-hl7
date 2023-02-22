package gov.cdc.ncezid.cloud.messaging.azure


import com.azure.storage.queue.QueueClientBuilder
import gov.cdc.ncezid.cloud.AzureConfig
import gov.cdc.ncezid.cloud.Providers
import gov.cdc.ncezid.cloud.messaging.CloudMessage
import gov.cdc.ncezid.cloud.messaging.CloudMessaging
import gov.cdc.ncezid.cloud.util.withMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.annotation.Requires
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import javax.inject.Singleton


@Singleton
@Requires(property = "azure.asq")
class ASQProxy(private val azureConfig: AzureConfig, private val meterRegistry: MeterRegistry? = null) : CloudMessaging {


    private val logger = LoggerFactory.getLogger(ASQProxy::class.java.name)

    override fun provider(): Providers = Providers.Azure

    private val asqClient = QueueClientBuilder()
        .connectionString(azureConfig.asq.connectionStr)
        .queueName(azureConfig.asq.queueName)
        .buildClient()

    override fun listQueues(vararg prefixes: String): List<String> {
        TODO("Not yet implemented")
    }

    override fun getQueueUrl(queueName: String): String {
        return azureConfig.asq.queueName!!
    }

    override fun getQueueUrl(): String {
        return azureConfig.asq.queueName!!
    }

    override fun receiveMessage(): List<CloudMessage> = meterRegistry.withMetrics("asq.receiveMessage") {
        val msg = asqClient.receiveMessage()
        listOf(ASQMessage(msg.messageId, msg.popReceipt, String(Base64.getDecoder().decode(msg.body.toString())), azureConfig.asq.queueName!!))
    }

    override fun receiveMessage(queueName: String): List<CloudMessage> = meterRegistry.withMetrics("asq.receiveMessage(queue) ") {
        val msg = asqClient.receiveMessage()
        listOf(ASQMessage(msg.messageId, msg.popReceipt, String(Base64.getDecoder().decode(msg.body.toString())), azureConfig.asq.queueName!!))
    }

    override fun deleteMessage(message: CloudMessage): String = meterRegistry.withMetrics("asq.deleteMessage") {
        kotlin.runCatching {
            asqClient.deleteMessage(message.id, message.recipientHandle)
            message.id
        }.onFailure {
            logger.error("Failed to delete message: {}. Exception: {}", message, it)
        }.getOrThrow()
    }


    override fun timeoutMessage(message: CloudMessage, timeout: Int): String = meterRegistry.withMetrics("asq.timeoutmessage") {
        kotlin.runCatching {
            asqClient.updateMessage(message.id, message.recipientHandle, "", Duration.ofSeconds(timeout.toLong()))
            message.id
        }.onFailure {
            logger.error("Failed to change message Visibility for message: {}. Exception: {}", message, it)
        }.getOrThrow()
    }

    override fun healthCheck(): String {
        TODO("Not yet implemented")
    }


}