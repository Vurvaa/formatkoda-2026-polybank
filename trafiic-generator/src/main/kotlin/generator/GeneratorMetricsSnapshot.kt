package ru.formatkoda.generator

data class GeneratorMetricsSnapshot(
    val startedTicks: Long,
    val completedTicks: Long,
    val failedTicks: Long,
    val sentRequests: Long
)