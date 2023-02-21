package gov.cdc.ncezid.cloud.messaging.aws

import gov.cdc.ncezid.cloud.AWSConfig
import gov.cdc.ncezid.cloud.Providers
import gov.cdc.ncezid.cloud.messaging.CloudMessage
import gov.cdc.ncezid.cloud.messaging.CloudMessaging
import gov.cdc.ncezid.cloud.util.stopTimer
import gov.cdc.ncezid.cloud.util.validateFor
import gov.cdc.ncezid.cloud.util.withMetrics
import gov.cdc.ncezid.cloud.util.withMetricsTimer
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.annotation.Requires

import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.retry.RetryMode
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import java.time.Duration
import javax.inject.Singleton


@Singleton
@Requires(property = "aws.sqs")
class SQSProxy(private val awsConfig: AWSConfig, private val meterRegistry: MeterRegistry? = null) : CloudMessaging {

    private val logger = LoggerFactory.getLogger(SQSProxy::class.java.name)

    override fun provider(): Providers = Providers.AWS

    private val sqsClient = SqsClient.builder().overrideConfiguration { cb ->
        cb.apiCallTimeout(Duration.ofSeconds(awsConfig.sqs.apiCallTimeoutSeconds))
            .apiCallAttemptTimeout(Duration.ofSeconds(awsConfig.sqs.apiCallAttemptTimeoutSeconds))
            .retryPolicy(RetryMode.STANDARD)
    }.region(Region.of(awsConfig.region)).build()

    private val queueUrl: String? = awsConfig.sqs.queueName?.let(::getQueueUrl)

    init {
        logger.info("AUDIT- Initializing AWS SQSProxy with config: {}", awsConfig)
    }

    /**
     * This was introduced to be able to provide a 'silent' call to the aws sqs api
     */
    override fun healthCheck(): String = meterRegistry.withMetrics("sqs.healthCheck") {
        sqsClient.getQueueUrl { it.queueName(awsConfig.sqs.queueName) }.responseMetadata().requestId()
    }

    override fun listQueues(vararg prefixes: String): List<String> = meterRegistry.withMetrics("sqs.listQueues") {
        logger.debug("Listing Queues matching prefixes: {}", prefixes)
        runCatching {
            when {
                prefixes.isEmpty() -> sqsClient.listQueues().queueUrls()
                else -> prefixes.flatMap { p -> sqsClient.listQueues { it.queueNamePrefix(p) }.queueUrls() }
            }
        }.onFailure {
            logger.error("Failed to list Queues matching prefixes: {}. Exception: {}", prefixes.joinToString(), it)
        }.getOrThrow()
    }

    override fun getQueueUrl(): String = awsConfig.sqs.queueName.validateFor("queueName") { getQueueUrl(it) }

    override fun getQueueUrl(queueName: String): String = meterRegistry.withMetrics("sqs.getQueueUrl") {
        runCatching {
            sqsClient.also {
                logger.debug("Getting QueueUrl for queueName: {}", queueName)
            }.getQueueUrl { it.queueName(queueName) }.queueUrl()
        }.onFailure {
            logger.error("Failed to get Queue Url for Queue Name: {}. Exception: {}", queueName, it)
        }.getOrThrow()
    }


    override fun receiveMessage(): List<CloudMessage> = queueUrl.validateFor("queueUrl") { receiveMessage(it) }

    override fun receiveMessage(queueNameOrUrl: String): List<CloudMessage> =
        meterRegistry.withMetricsTimer("sqs.receiveMessage.poll") { timer ->
            kotlin.runCatching {
                when {
                    queueNameOrUrl.startsWith("https://sqs") -> queueNameOrUrl
                    else -> getQueueUrl(queueNameOrUrl)
                }.let { url ->
                    logger.trace("Receiving messages on queue: {}", queueNameOrUrl)
                    sqsClient.receiveMessage {
                        it.queueUrl(url)
                            .maxNumberOfMessages(awsConfig.sqs.maxNumberOfMessages)
                            .waitTimeSeconds(awsConfig.sqs.waitTimeSeconds)
                    }.messages().map {
                        SQSMessage(it.messageId(), it.receiptHandle(), it.body(), url)
                    }.also {
                        // This forces a 'timed' execution for only polls that actually receive messages
                        if (it.isNotEmpty())
                            meterRegistry?.stopTimer(timer, "sqs.receiveMessage")
                    }
                }
            }.onFailure {
                logger.error("Failed to receive message on queueUrl: {}. Exception: {}", queueNameOrUrl, it)
            }.getOrThrow()
        }

    override fun deleteMessage(message: CloudMessage): String = meterRegistry.withMetrics("sqs.deleteMessage") {
        kotlin.runCatching {
            sqsClient.also { logger.debug("Deleting message: {}", message) }
                .deleteMessage { it.queueUrl(message.queue).receiptHandle(message.recipientHandle) }
                .responseMetadata().requestId()
        }.onFailure {
            logger.error("Failed to delete message: {}. Exception: {}", message, it)
        }.getOrThrow()
    }

    override fun timeoutMessage(message: CloudMessage, timeout: Int): String =
        meterRegistry.withMetrics("sqs.timeoutMessage") {
            runCatching {
                sqsClient.also {
                    logger.debug("Updating message: {} with timeout: {}", message, timeout)
                }.changeMessageVisibility {
                    it.queueUrl(message.queue)
                        .receiptHandle(message.recipientHandle)
                        .visibilityTimeout(timeout)
                }.responseMetadata().requestId()
            }.onFailure {
                logger.error(
                    "Failed to set message visibility timeout [{}] for message: {}. Exception: {}",
                    timeout, message, it
                )
            }.getOrThrow()
        }

    override fun toString(): String {
        return "SQSProxy(sqsClient=$sqsClient)"
    }
}

