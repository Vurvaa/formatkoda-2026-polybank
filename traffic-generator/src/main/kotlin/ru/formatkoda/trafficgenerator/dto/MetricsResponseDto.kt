package ru.formatkoda.trafficgenerator.dto

import kotlinx.serialization.Serializable

@Serializable
data class MetricsResponseDto(
    val requestsPerSecond: Long,
    val ticksPerSecond: Long,
    val sentRequests: Long,
    val failedTicks: Long
)
