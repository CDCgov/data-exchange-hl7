package gov.cdc.ncezid.cloud

import io.micronaut.context.annotation.ConfigurationBuilder
import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("azure")
class AzureConfig {

    @ConfigurationBuilder(configurationPrefix = "blob")
    val blob: BlobConfig = BlobConfig()

    @ConfigurationBuilder(configurationPrefix = "blob.health")
    val blobHealth: HealthConfig = HealthConfig()

    @ConfigurationBuilder(configurationPrefix = "asq")
    val asq: ASQConfig = ASQConfig()

    @ConfigurationBuilder(configurationPrefix = "asq.health")
    var sqsHealth: HealthConfig = HealthConfig()


    inner class BlobConfig {
        var container: String? = null
        var connectStr: String? = null

        override fun toString(): String {
            return "container='$container', connectStr='$connectStr', health=${blobHealth.enabled}"
        }
    }

    inner class ASQConfig {
        var queueName: String? = null
        var connectionStr: String? = null
        var apiCallTimeoutSeconds: Long = 60
        var apiCallAttemptTimeoutSeconds: Long = 20
        var maxNumberOfMessages: Int = 1
        var waitTimeSeconds: Int = 5


        override fun toString(): String {
            return "queueName='$queueName', " +
                    "connectSTR='$connectionStr'," +
                    "apiCallTimeoutSeconds='$apiCallTimeoutSeconds', " +
                    "apiCallAttemptTimeoutSeconds='$apiCallAttemptTimeoutSeconds', " +
                    "maxNumberOfMessages='$maxNumberOfMessages', " +
                    "waitTimeSeconds='$waitTimeSeconds', " +
                    "health=${sqsHealth.enabled}"
        }
    }

    class HealthConfig {
        var enabled: Boolean = false
    }

    override fun toString(): String {
        return """
            |AzureConfig(
            |  blob='[$blob]'
            |  asq='[$asq]'
            |)""".trimMargin()
    }

}
