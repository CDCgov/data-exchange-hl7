package gov.cdc.dex.hl7

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import com.google.gson.JsonObject
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.azure.health.*
import java.time.LocalDate

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
        val evHubReceiveName: String = System.getenv("EventHubReceiveName")
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val depChecker = DependencyChecker()
        val time = measureTime {
            addToResult(depChecker.checkEventHub(evHubConnStr, evHubReceiveName), result)
            addToResult(depChecker.checkEventHub(evHubConnStr, evHubSendName), result)
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
    private fun addToResult(dependencyData: DependencyHealthData, result: HealthCheckResult) {
        result.dependencyHealthChecks.add(dependencyData)
        if (dependencyData.status != "UP") result.status = "DOWNGRADED"
    }
}