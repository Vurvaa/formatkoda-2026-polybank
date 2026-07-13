package ru.formatkoda.generator

import java.util.concurrent.atomic.AtomicLong

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

    fun snapshot(): GeneratorMetricsSnapshot {
        return GeneratorMetricsSnapshot(
            startedTicks = startedTicks.get(),
            completedTicks = completedTicks.get(),
            failedTicks = failedTicks.get(),
            sentRequests = sentRequests.get()
        )
    }
}