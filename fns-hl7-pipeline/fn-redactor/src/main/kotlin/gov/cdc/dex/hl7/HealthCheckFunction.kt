package gov.cdc.dex.hl7

import com.azure.identity.DefaultAzureCredentialBuilder
import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import gov.cdc.dex.azure.health.*
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
        val evHubNamespace = System.getenv("EventHubConnection__fullyQualifiedNamespace")
        val entraClientId = System.getenv("EventHubConnection__clientId")
        val tokenCredential = DefaultAzureCredentialBuilder()
            .managedIdentityClientId(entraClientId)
            .build()
        val time = measureTime {
            val ehHealthData = DependencyChecker().checkEventHub(evHubNamespace, evHubSendName, tokenCredential)
            result.dependencyHealthChecks.add(ehHealthData)
            result.status = if (ehHealthData.status == "UP") "UP" else "DOWNGRADED"
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


}