package gov.cdc.ncezid.cloud.util

import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthResult
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("gov.cdc.ncezid.cloud.util.HealthUtil")

// TODO - move this to a general purpose location for all health checks
fun withDetails(name: String, block: () -> Pair<HealthStatus, Map<String, String?>>): Publisher<HealthResult> {
    return System.currentTimeMillis().let { startTime ->
        try {
            block()
        } catch (e: Exception) {
            logger.error("$name Health check failed.", e)
            HealthStatus.DOWN to mapOf(
                "message" to e.message
            )
        }.let {
            Flowable.just(
                HealthResult.builder(name, it.first)
                    .details(it.second.plus("latency" to "${System.currentTimeMillis() - startTime} ms"))
                    .build()
            )
        }
    }
}
