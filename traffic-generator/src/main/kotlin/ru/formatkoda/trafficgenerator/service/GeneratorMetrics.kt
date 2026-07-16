package ru.formatkoda.trafficgenerator.service

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import ru.formatkoda.trafficgenerator.domain.GeneratorStatus
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

data class GeneratorMetricsRaw(
    val startedTicks: Long,
    val completedTicks: Long,
    val failedTicks: Long,
    val sentRequests: Long
)

data class MetricsSnapshot(
    val requestsPerSecond: Long,
    val ticksPerSecond: Long,
    val sentRequests: Long,
    val failedTicks: Long
)

class GeneratorMetrics(registry: MeterRegistry) {
    private val startedTicks = AtomicLong()
    private val completedTicks = AtomicLong()
    private val failedTicks = AtomicLong()
    private val sentRequests = AtomicLong()

    private val status = AtomicReference(GeneratorStatus.STOPPED)

    init {
        Gauge.builder(
            "generator.ticks.started",
            startedTicks
        ) { value ->
            value.get().toDouble()
        }.register(registry)

        Gauge.builder(
            "generator.ticks.completed",
            completedTicks
        ) { value ->
            value.get().toDouble()
        }.register(registry)

        Gauge.builder(
            "generator.ticks.failed",
            failedTicks
        ) { value ->
            value.get().toDouble()
        }.register(registry)

        Gauge.builder(
            "generator.requests.sent",
            sentRequests
        ) { value ->
            value.get().toDouble()
        }.register(registry)

        GeneratorStatus.entries.forEach { expectedStatus ->
            Gauge.builder("generator.status") {
                if (status.get() == expectedStatus) 1.0 else 0.0
            }
                .tag("status", expectedStatus.name)
                .register(registry)
        }
    }

    fun tickStarted() {
        startedTicks.incrementAndGet()
    }

    fun tickCompleted() {
        completedTicks.incrementAndGet()
    }

    fun tickFailed() {
        failedTicks.incrementAndGet()
    }

    fun requestSent() {
        sentRequests.incrementAndGet()
    }

    fun statusChanged(newStatus: GeneratorStatus) {
        status.set(newStatus)
    }

    fun snapshot(): GeneratorMetricsRaw {
        return GeneratorMetricsRaw(
            startedTicks = startedTicks.get(),
            completedTicks = completedTicks.get(),
            failedTicks = failedTicks.get(),
            sentRequests = sentRequests.get()
        )
    }
}