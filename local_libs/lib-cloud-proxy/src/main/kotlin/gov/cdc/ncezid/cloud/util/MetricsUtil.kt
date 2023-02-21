package gov.cdc.ncezid.cloud.util

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("gov.cdc.ncezid.cloud.util.MetricsUtil")

// TODO - These could be moved to a separate library for metrics
fun <R> MeterRegistry?.withMetrics(name: String, block: () -> R): R =
    this?.startTimer()?.let { start ->
        logger.trace("Metrics enabled! Started timer for '$name' and now executing code block")
        runCatching { block() }
            .onSuccess { stopMeter(start, name) }
            .onFailure { stopMeter(start, name, true) }
            .getOrThrow()
    } ?: block().also { logger.trace("Metrics are disabled! Executing code block") }

/**
 * This extension fn instruments your code block with metrics, but allows the code block additional control
 * over stopping any custom internal timers
 */
fun <R> MeterRegistry?.withMetricsTimer(name: String, block: (timer: Timer.Sample?) -> R): R =
    this?.startTimer()?.let { start ->
        logger.trace("Metrics enabled! Started timer for '$name' and now executing code block")
        runCatching { block(start) }
            .onSuccess { stopMeter(start, name) }
            .onFailure { stopMeter(start, name, true) }
            .getOrThrow()
    } ?: block(null).also { logger.trace("Metrics are disabled! Executing code block") }

fun MeterRegistry.stopMeter(timer: Timer.Sample?, name: String, fromError: Boolean = false) {
    this.stopTimer(timer, name, fromError).also {
        logger.trace("Recorded metric [{}] in {} ms", name.timerName(fromError), it.nsToMs())
    }
    this.gauge(name.gaugeName(), if (fromError) 1 else 0)
}

fun MeterRegistry.startTimer(): Timer.Sample? = Timer.start(this)
fun MeterRegistry.stopTimer(timer: Timer.Sample?, name: String, fromError: Boolean = false): Long? =
    timer?.stop(this.timer(name.timerName(fromError)))

fun String.gaugeName() = "$this.status.gauge".toLowerCase()
fun String.timerName(fromError: Boolean = false) = when (fromError) {
    true -> "$this.error.time"
    else -> "$this.time"
}.toLowerCase()

private fun Long?.nsToMs() = TimeUnit.NANOSECONDS.toMillis(this ?: 0)
