package gov.cdc.ncezid.cloud

import io.micronaut.context.annotation.ConfigurationBuilder
import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("aws")
class AWSConfig {
    var region: String = "us-east-1"

    @ConfigurationBuilder(configurationPrefix = "s3")
    val s3: S3Config = S3Config()

    @ConfigurationBuilder(configurationPrefix = "s3.health")
    var s3Health: HealthConfig = HealthConfig()

    @ConfigurationBuilder(configurationPrefix = "sqs")
    val sqs: SQSConfig = SQSConfig()

    @ConfigurationBuilder(configurationPrefix = "sqs.health")
    var sqsHealth: HealthConfig = HealthConfig()

    inner class S3Config {
        var bucket: String? = null
        var apiCallTimeoutSeconds: Long = 60
        var apiCallAttemptTimeoutSeconds: Long = 20

        override fun toString(): String {
            return "bucket='$bucket', " +
                    "apiCallTimeoutSeconds='$apiCallTimeoutSeconds', " +
                    "apiCallAttemptTimeoutSeconds='$apiCallAttemptTimeoutSeconds', " +
                    "health=${s3Health.enabled}"
        }
    }

    inner class SQSConfig {
        var queueName: String? = null
        var apiCallTimeoutSeconds: Long = 60
        var apiCallAttemptTimeoutSeconds: Long = 20
        var maxNumberOfMessages: Int = 1
        var waitTimeSeconds: Int = 5

        override fun toString(): String {
            return "queueName='$queueName', " +
                    "apiCallTimeoutSeconds='$apiCallTimeoutSeconds', " +
                    "apiCallAttemptTimeoutSeconds='$apiCallAttemptTimeoutSeconds', " +
                    "maxNumberOfMessages='$maxNumberOfMessages', " +
                    "waitTimeSeconds='$waitTimeSeconds', " +
                    "health=${sqsHealth.enabled}"
        }
    }

    inner class HealthConfig {
        var enabled: Boolean = false
    }

    override fun toString(): String {
        return """
            |AWSConfig(
            |  region='$region', 
            |  s3=[$s3], 
            |  sqs=[$sqs]
            |)""".trimMargin()
    }
}

