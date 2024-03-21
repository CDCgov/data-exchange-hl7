package gov.cdc.dex.hl7

import com.microsoft.azure.functions.*
import com.microsoft.azure.functions.annotation.*
import com.azure.messaging.eventhubs.*
import com.google.gson.JsonObject
import gov.cdc.dex.util.JsonHelper.toJsonElement
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
        val isHealthy  = checkEventHubHealth(evHubConnStr, evHubSendName)
        var eventHubStatus = "DOWNGRADED"
        var issues = "Service is down"
        if (isHealthy) {
            eventHubStatus = "UP"
            issues = ""
        }

        val period = java.time.Period.between(LocalDate.now(), startTime)
        val result = JsonObject()
        val deps = JsonObject()

        result.add("status", "UP".toJsonElement())
        result.add("total_checks_duration", period.toString().toJsonElement())
        deps.add("service", "EventHub".toJsonElement())
        deps.add("status", eventHubStatus.toJsonElement())
        deps.add("health_issues", issues.toJsonElement())
        result.add("dependency_health_checks", deps)

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(result)
                .build()
    }


    private fun checkEventHubHealth(connectionString: String, eventHubName: String): Boolean {
        // Set up EventProcessor to receive events from the Event Hub
        val eventProcessor = EventProcessorClientBuilder()
            .connectionString(connectionString, eventHubName)
            .buildEventProcessorClient()

        // Connect to Event Hub and check if it's healthy
        return try {
            if (!eventProcessor.isRunning) {
                // Start the EventProcessor to further test connectivity
                eventProcessor.start()
            }
            // Event Hub is considered healthy if no exceptions are thrown
            true
        } catch (e: Exception) {
            false
        } finally {
            if (eventProcessor.isRunning) {
                eventProcessor.stop()
            }
        }
    }
}