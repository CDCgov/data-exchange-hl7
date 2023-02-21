package gov.cdc.ncezid.cloud.messaging.aws

import gov.cdc.ncezid.cloud.util.withDetails
import io.micronaut.context.annotation.Requires
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthIndicator
import io.micronaut.management.health.indicator.HealthResult
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
@Requires(property = "aws.sqs.health.enabled", value = "true", defaultValue = "false")
class SQSHealth(private val sqsProxy: SQSProxy) : HealthIndicator {

    private val logger = LoggerFactory.getLogger(SQSHealth::class.java)
    private val queueUrl: String = sqsProxy.getQueueUrl()

    init {
        logger.info("Initializing SQSHealth for queueUrl: {}", queueUrl)
    }

    override fun getResult(): Publisher<HealthResult> = withDetails("sqs") {
        sqsProxy.healthCheck().let {
            logger.trace("Successfully retrieved queue: $it")
            HealthStatus.UP to mapOf(
                "url" to queueUrl,
                "requestId" to it,
                "message" to "UP"
            )
        }
    }
}