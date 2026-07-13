package ru.formatkoda.generator

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

class GeneratorService(
    private val engine: GeneratorEngine,
    private val config: GeneratorConfig,
    private val metrics: GeneratorMetrics
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GeneratorService::class.java)
    }

    suspend fun run() {
        warmUp()

        logger.debug("warmup completed")

        coroutineScope {
            launch {
                logMetrics()
            }

            repeat(config.workers) { workerId ->
                launch {
                    runWorker(workerId)
                }
            }
        }
    }

    private suspend fun runWorker(workerId: Int) {
        while (currentCoroutineContext().isActive) {
            metrics.tickStarted()

            try {
                engine.tick()
                metrics.tickCompleted()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                metrics.tickFailed()

                logger.error(
                    "generator worker {} failed",
                    workerId,
                    error
                )
            }

            delay(config.tickDelay)
        }
    }

    private suspend fun logMetrics() {
        var previousRequests = 0L
        var previousTicks = 0L

        while (currentCoroutineContext().isActive) {
            delay(1.seconds)

            val snapshot = metrics.snapshot()
            val requestsPerSecond = snapshot.sentRequests - previousRequests
            val ticksPerSecond = snapshot.completedTicks - previousTicks

            previousRequests = snapshot.sentRequests
            previousTicks = snapshot.completedTicks

            logger.info(
                "rate: {} requests/s, {} ticks/s, total requests: {}, failed ticks: {}",
                requestsPerSecond,
                ticksPerSecond,
                snapshot.sentRequests,
                snapshot.failedTicks
            )
        }
    }

    private suspend fun warmUp() {
        repeat(config.warmupUserCountRange.random()) {
            try {
                engine.warmUpTick()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logger.error("warm-up iteration failed", error)
            }
        }
    }
}