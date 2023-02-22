package gov.cdc.ncezid.cloud.storage.aws

import gov.cdc.ncezid.cloud.AWSConfig
import gov.cdc.ncezid.cloud.util.withDetails
import io.micronaut.context.annotation.Requires
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthIndicator
import io.micronaut.management.health.indicator.HealthResult
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
@Requires(property = "aws.s3.health.enabled", value = "true", defaultValue = "false")
class S3Health(
    private val s3Proxy: S3Proxy,
    private val awsConfig: AWSConfig
) : HealthIndicator {

    private val logger = LoggerFactory.getLogger(S3Health::class.java)

    init {
        logger.info("Initializing S3Health for bucket: {}", awsConfig.s3.bucket)
    }

    override fun getResult(): Publisher<HealthResult> = withDetails("s3") {
        s3Proxy.healthCheck().let {
            logger.trace("Successfully validated reachability for bucket: {}. ResponseCode: {}", awsConfig.s3.bucket, it)
            HealthStatus.UP to mapOf(
                "bucket" to awsConfig.s3.bucket,
                "requestId" to it,
                "message" to "UP"
            )
        }
    }
}