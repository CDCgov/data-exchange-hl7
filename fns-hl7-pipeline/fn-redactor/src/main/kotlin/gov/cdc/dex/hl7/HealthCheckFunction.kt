package gov.cdc.dex.hl7

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import com.google.gson.JsonObject
import gov.cdc.dex.util.JsonHelper.toJsonElement
import gov.cdc.dex.azure.health.*
import java.time.LocalDate

import java.util.*

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
        val startTime = LocalDate.now()
        val evHubSendName: String = System.getenv("EventHubSendName")
        val evHubConnStr = System.getenv("EventHubConnectionString")
        val result = DependencyChecker().checkEventHub()

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(result)
                .build()
    }


}