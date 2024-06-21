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
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val evHubReceiveName = System.getenv("EventHubReceiveName")
        val checker = DependencyChecker()
        val time = measureTime {
            addToResult(checker.checkEventHub(evHubConnStr, evHubReceiveName), result)
            addToResult(checker.checkEventHub(Function.fnConfig.evHubSender), result)
        }
        result.totalChecksDuration = time.toComponents { hours, minutes, seconds, nanoseconds ->
            "%02d:%02d:%02d.%03d".format(hours, minutes, seconds, nanoseconds / 1000000)
        }
        return request
            .createResponseBuilder(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body(result)
            .build();
    }
    private fun addToResult(dependencyData: DependencyHealthData, result: HealthCheckResult) {
        result.dependencyHealthChecks.add(dependencyData)
        if (dependencyData.status != "UP") result.status = "DOWNGRADED"
    }
}