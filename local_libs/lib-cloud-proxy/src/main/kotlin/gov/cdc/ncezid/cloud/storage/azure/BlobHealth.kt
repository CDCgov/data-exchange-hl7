package gov.cdc.ncezid.cloud.storage.azure

import gov.cdc.ncezid.cloud.AzureConfig
import gov.cdc.ncezid.cloud.messaging.aws.SQSHealth
import gov.cdc.ncezid.cloud.util.withDetails
import io.micronaut.context.annotation.Requires
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthIndicator
import io.micronaut.management.health.indicator.HealthResult
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
@Requires(property = "azure.blob.health.enabled", value = "true", defaultValue = "false")
class BlobHealth(private val blobProxy: BlobProxy, private val azureConfig: AzureConfig) : HealthIndicator {

    private val logger = LoggerFactory.getLogger(SQSHealth::class.java)

    init {
        logger.info(
            "Initializing BlobHealth for container: {} with connectionString: {}",
            azureConfig.blob.container,
            azureConfig.blob.connectStr
        )
    }

    override fun getResult(): Publisher<HealthResult> = withDetails("blob") {
        blobProxy.healthCheck().let {
            logger.trace("Successfully retrieved queue: $it")
            HealthStatus.UP to mapOf(
                "container" to azureConfig.blob.container,
                "connectionString" to azureConfig.blob.connectStr,
                "requestId" to it,
                "message" to "UP"
            )
        }
    }
}