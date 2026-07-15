package ru.formatkoda.trafficgenerator.service

import java.util.concurrent.atomic.AtomicLong

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

class GeneratorMetrics {
    private val startedTicks = AtomicLong()
    private val completedTicks = AtomicLong()
    private val failedTicks = AtomicLong()
    private val sentRequests = AtomicLong()

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

    fun snapshot(): GeneratorMetricsRaw {
        return GeneratorMetricsRaw(
            startedTicks = startedTicks.get(),
            completedTicks = completedTicks.get(),
            failedTicks = failedTicks.get(),
            sentRequests = sentRequests.get()
        )
    }
}