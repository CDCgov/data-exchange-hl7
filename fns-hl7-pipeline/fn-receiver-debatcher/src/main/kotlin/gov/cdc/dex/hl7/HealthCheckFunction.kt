package gov.cdc.dex.hl7

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.health.DependencyChecker
import gov.cdc.dex.azure.health.DependencyHealthData
import gov.cdc.dex.azure.health.HealthCheckResult
import java.util.*
import kotlin.time.measureTime

class HealthCheckFunction {
    @FunctionName("health")
    fun healthCheck(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.ANONYMOUS
        )
        request: HttpRequestMessage<Optional<String>>
    ): HttpResponseMessage {
        val result = HealthCheckResult()
        val evHubSendName: String = System.getenv("EventHubSendName")
        val evHubReportsName: String =System.getenv("EventReportsHubName")
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val blobIngestConn = System.getenv("BlobIngestConnectionString")
        val blobContainer = System.getenv("BlobIngestContainerName")
        val blobAttachments = System.getenv("attachmentBlobConnString")
        val queueName = System.getenv("queueName")
        val depChecker = DependencyChecker()
        val time = measureTime {
            addToResult(depChecker.checkEventHub(evHubConnStr, evHubSendName), evHubSendName, result)
            addToResult(depChecker.checkEventHub(evHubConnStr, evHubReportsName), evHubReportsName, result)
            addToResult(depChecker.checkStorageAccount(blobIngestConn, blobContainer), getAccountName(blobIngestConn), result)
            addToResult(depChecker.checkStorageAccount(blobAttachments), getAccountName(blobAttachments), result)
            addToResult(depChecker.checkStorageQueue(blobIngestConn, queueName), queueName, result)
        }
        result.totalChecksDuration = time.toComponents { hours, minutes, seconds, nanoseconds ->
            "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, nanoseconds / 1000000)
        }

        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build()
    }

    private fun addToResult(dependencyData: DependencyHealthData, resourceName: String,  result: HealthCheckResult) {
        dependencyData.resourceName = resourceName
        result.dependencyHealthChecks.add(dependencyData)
        if (dependencyData.status != "UP") result.status = "DOWNGRADED"
    }

    private fun getAccountName(connectionString: String) : String {
        return connectionString.substringAfter("AccountName=").substringBefore(";")
    }
}