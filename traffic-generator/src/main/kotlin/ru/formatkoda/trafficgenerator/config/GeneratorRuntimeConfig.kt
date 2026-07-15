package ru.formatkoda.trafficgenerator.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class GeneratorRuntimeConfig(
    val workers: Int = 10,
    val tickDelay: Duration = 100.milliseconds,
    val warmupUserCount: Int = 30
) {
    init {
        require(workers > 0) {
            "workers must be positive"
        }

        require(!tickDelay.isNegative()) {
            "tickDelay must not be negative"
        }

        require(warmupUserCount >= 0) {
            "warmupUserCount must not be negative"
        }
    }
}
