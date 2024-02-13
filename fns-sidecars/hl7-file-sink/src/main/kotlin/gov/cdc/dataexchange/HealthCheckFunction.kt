package gov.cdc.dataexchange

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
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
        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body("UP")
                .build();
    }
}