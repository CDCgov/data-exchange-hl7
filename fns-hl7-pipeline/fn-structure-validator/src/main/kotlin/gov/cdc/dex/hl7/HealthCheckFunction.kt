package gov.cdc.dex.hl7

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.health.DependencyChecker
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
        val time = measureTime {
            result.dependencyHealthChecks.add(DependencyChecker().checkEventHub(
                ValidatorFunction.fnConfig.evHubSender
            ))
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
}